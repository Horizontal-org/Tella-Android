package rs.readahead.washington.mobile.views.activity.viewer

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.databinding.ActivityAudioPlayBinding
import rs.readahead.washington.mobile.media.AudioPlayer
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.util.ThreadUtil
import rs.readahead.washington.mobile.views.activity.MetadataViewerActivity
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.initContracts
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import java.util.*
import java.util.concurrent.TimeUnit

class AudioPlayActivity : BaseLockActivity(), StyledPlayerView.ControllerVisibilityListener {
    var mPlay: ImageButton? = null
    var mRwd: ImageButton? = null
    var mFwd: ImageButton? = null
    var mTimer: TextView? = null
    var mDuration: TextView? = null
    var forward: View? = null
    var rewind: View? = null
    private var handlingVaultFile: VaultFile? = null
    private var audioPlayer: AudioPlayer? = null
    private var audioPlayerListener: AudioPlayer.Listener? = null

    private var showActions = false
    private var actionsDisabled = false
    private var withMetadata = false
    private var alertDialog: AlertDialog? = null
    private var progressDialog: ProgressDialog? = null
    private var paused = true
    private lateinit var toolbar: Toolbar
    private var isInfoShown = false
    private lateinit var binding: ActivityAudioPlayBinding
    private val viewModel: SharedMediaFileViewModel by viewModels()

    companion object {
        const val PLAY_MEDIA_FILE = "pmf"
        const val PLAY_MEDIA_FILE_ID_KEY = "pmfik"
        const val NO_ACTIONS = "na"
        private const val TIME_FORMAT = "%02d:%02d:%02d"
        private const val SEEK_DELAY = 15000
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        actionsDisabled = intent.hasExtra(VideoViewerActivity.NO_ACTIONS)

        initView()
        initListeners()
        initContracts()
        setupToolbar()
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out)
        enablePlay()
        initVaultMediaFile()
        initAudioListener()
        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {
            error.observe(this@AudioPlayActivity) {
                onShowError(it)
            }
            onMediaFileExportStatus.observe(this@AudioPlayActivity) { status ->
                when (status) {
                    MediaFileExportStatus.EXPORT_START -> onExportStarted()
                    MediaFileExportStatus.EXPORT_PROGRESS -> onMediaExported()
                    MediaFileExportStatus.EXPORT_END -> onExportEnded()
                }
            }
            onMediaFileDeleted.observe(this@AudioPlayActivity) { deleted ->
                if (deleted) onMediaFileDeleted()
            }
            onMediaFileDeleteConfirmed.observe(this@AudioPlayActivity) { mediaFileDeletedConfirmation ->
                mediaFileDeletedConfirmation.vaultFile?.let { deletedVaultFile ->
                    onMediaFileDeleteConfirmation(
                        deletedVaultFile,
                        mediaFileDeletedConfirmation.showConfirmDelete
                    )
                }
            }
            onMediaFileRenamed.observe(this@AudioPlayActivity) { renamed ->
                onMediaFileRename(renamed)
            }
            onMediaFileGot.observe(this@AudioPlayActivity) { renamed ->
                onMediaFileSuccess(renamed)
            }
        }
    }

    private fun initVaultMediaFile() {
        if (intent.hasExtra(Companion.PLAY_MEDIA_FILE)) {
            val vaultFile =
                intent.getSerializableExtra(Companion.PLAY_MEDIA_FILE) as VaultFile?
            if (vaultFile != null) {
                ThreadUtil.runOnMain {
                    onMediaFileSuccess(
                        vaultFile
                    )
                }
            }
        } else if (intent.hasExtra(Companion.PLAY_MEDIA_FILE_ID_KEY)) {
            val id = intent.getStringExtra(Companion.PLAY_MEDIA_FILE_ID_KEY)
            if (id != null) {
                viewModel!!.getMediaFile(id)
            }
        }
    }

    private fun initAudioListener() {
        audioPlayerListener = object : AudioPlayer.Listener {
            override fun onStart(duration: Int) {
                mDuration!!.text = timeToString(duration.toLong())
            }

            override fun onStop() {
                stopPlayer()
                paused = true
                enablePlay()
                showTimeRemaining(0)
            }

            override fun onProgress(currentPosition: Int) {
                showTimeRemaining(currentPosition)
            }

            private fun showTimeRemaining(left: Int) {
                mTimer!!.text = timeToString(left.toLong())
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start)
    }


    private fun onShowError(errorResId: Int) {
        DialogUtils.showBottomMessage(
            this, getString(errorResId), true
        )
    }


    private fun initListeners() {
        mPlay!!.setOnClickListener {
            if (paused) {
                handlePlay()
            } else {
                handlePause()
            }
        }
        forward!!.setOnClickListener {
            audioPlayer!!.ffwd(
                SEEK_DELAY
            )
        }
        rewind!!.setOnClickListener {
            audioPlayer!!.rwd(
                SEEK_DELAY
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (isInfoShown) {
            toolbar.menu.findItem(R.id.menu_item_more).isVisible = true
            toolbar.menu.findItem(R.id.menu_item_metadata).isVisible = true
            toolbar.title = handlingVaultFile!!.name
        } else {
            stopPlayer()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        handlePause()
    }

    override fun onDestroy() {
        audioPlayerListener = null
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
        hideProgressDialog()
        super.onDestroy()
    }


    private fun onMediaFileSuccess(vaultFile: VaultFile) {
        handlingVaultFile = vaultFile
        toolbar.title = vaultFile.name
        //handlePlay();
        if (!actionsDisabled) {
            showActions = true
            invalidateOptionsMenu()
        }
    }

    fun onMediaExported() {
        showToast(resources.getQuantityString(R.plurals.gallery_toast_files_exported, 1, 1))
    }

    fun onExportError(error: Throwable) {
        showToast(R.string.gallery_toast_fail_exporting_to_device)
    }

    fun onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(
            this,
            getString(R.string.gallery_save_to_device_dialog_progress_expl)
        )
    }

    fun onExportEnded() {
        hideProgressDialog()
    }

    private fun setupToolbar() {
        toolbar = binding.toolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.video_view_menu)
            handlingVaultFile?.let { file ->
                setupMetadataMenuItem(file.metadata != null)
            }
            if (handlingVaultFile != null && handlingVaultFile!!.metadata != null) {
                val item = toolbar.menu.findItem(R.id.menu_item_metadata)
                item.isVisible = true
            }
            toolbar.menu.findItem(R.id.menu_item_more)
                .setOnMenuItemClickListener {
                    handlingVaultFile?.let { it1 ->
                        showVaultActionsDialog(
                            it1,
                            viewModel,
                            {
                                isInfoShown = true
                                onVisibilityChanged(View.VISIBLE)
                            },
                            toolbar = toolbar
                        )
                    }
                    false
                }
        }
    }

    private fun setupMetadataMenuItem(visible: Boolean) {
        if (actionsDisabled) {
            return
        }
        val mdMenuItem = toolbar.menu.findItem(R.id.menu_item_metadata)
        mdMenuItem.isVisible = visible
        if (visible) {
            mdMenuItem.setOnMenuItemClickListener {
                showMetadata()
                false
            }
        }
    }

    override fun onVisibilityChanged(visibility: Int) {
        toolbar.visibility = if (!isInfoShown) visibility else View.VISIBLE
    }


    private fun onMediaFileDeleteConfirmation(vaultFile: VaultFile, showConfirmDelete: Boolean) {
        if (showConfirmDelete) {
            showConfirmSheet(
                supportFragmentManager,
                getString(R.string.Vault_Warning_Title),
                getString(R.string.Vault_Confirm_delete_Description),
                getString(R.string.Vault_Delete_anyway),
                getString(R.string.action_cancel),
                object : ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) {
                        if (isConfirmed) {
                            viewModel.deleteMediaFiles(vaultFile)
                        }
                    }
                }
            )
        } else {
            viewModel.deleteMediaFiles(vaultFile)
        }
    }

    fun onMediaFileDeleted() {
        MyApplication.bus().post(MediaFileDeletedEvent())
        finish()
    }

    fun onMediaFileDeletionError(throwable: Throwable) {
        showToast(R.string.gallery_toast_fail_deleting_files)
    }

    private fun onMediaFileRename(vaultFile: VaultFile) {
        toolbar.title = vaultFile.name
        MyApplication.bus().post(VaultFileRenameEvent())
    }

    fun onMediaFileRenameError(throwable: Throwable) {}

    fun getContext(): Context {
        return this
    }


    private fun handlePlay() {
        if (handlingVaultFile == null) {
            return
        }
        if (audioPlayer != null) {
            audioPlayer!!.resume()
        } else {
            audioPlayer = AudioPlayer(
                this,
                audioPlayerListener!!
            )
            audioPlayer!!.play(handlingVaultFile)
        }
        paused = false
        disablePlay()
        disableScreenTimeout()
    }

    private fun handlePause() {
        if (handlingVaultFile == null) {
            return
        }
        enablePlay()
        paused = true
        if (audioPlayer != null) {
            audioPlayer!!.pause()
        }
        enableScreenTimeout()
    }

    private fun onPlayerStop() {
        enablePlay()
    }

    private fun disablePlay() {
        mPlay!!.setImageDrawable(this.resources.getDrawable(R.drawable.big_white_pause_24p))
        enableButton(forward, mFwd)
        enableButton(rewind, mRwd)
    }

    private fun enablePlay() {
        mPlay!!.setImageDrawable(this.resources.getDrawable(R.drawable.ic_play_arrow_white_24dp))
        disableButton(forward, mFwd)
        disableButton(rewind, mRwd)
    }

    private fun enableButton(view: View?, button: ImageButton?) {
        button!!.isClickable = true
        view!!.alpha = 1f
    }

    private fun disableButton(view: View?, button: ImageButton?) {
        button!!.isClickable = false
        view!!.alpha = .3f
    }

    private fun stopPlayer() {
        if (audioPlayer != null) {
            audioPlayer!!.stop()
            audioPlayer = null
            onPlayerStop()
            enableScreenTimeout()
        }
    }

    private fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    private fun showMetadata() {
        val viewMetadata = Intent(this, MetadataViewerActivity::class.java)
        viewMetadata.putExtra(Metadata.VIEW_METADATA, handlingVaultFile)
        startActivity(viewMetadata)
    }

    private fun timeToString(duration: Long): String {
        return String.format(
            Locale.ROOT, Companion.TIME_FORMAT,
            TimeUnit.MILLISECONDS.toHours(duration),
            TimeUnit.MILLISECONDS.toMinutes(duration) -
                    TimeUnit.MINUTES.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
            TimeUnit.MILLISECONDS.toSeconds(duration) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        )
    }

    private fun disableScreenTimeout() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun enableScreenTimeout() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initView() {
        mPlay = binding!!.content.playAudio
        mRwd = binding!!.content.rwdButton
        mFwd = binding!!.content.rwdButton
        mTimer = binding!!.content.audioTime
        mDuration = binding!!.content.duration
        forward = binding!!.content.fwdButton
        rewind = binding!!.content.rwdButton
        toolbar = binding!!.toolbar
    }


}