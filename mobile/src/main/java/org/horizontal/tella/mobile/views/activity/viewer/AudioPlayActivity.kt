package org.horizontal.tella.mobile.views.activity.viewer

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.event.MediaFileDeletedEvent
import org.horizontal.tella.mobile.bus.event.VaultFileRenameEvent
import org.horizontal.tella.mobile.databinding.ActivityAudioPlayBinding
import org.horizontal.tella.mobile.media.AudioPlayer
import org.horizontal.tella.mobile.util.DialogsUtil
import org.horizontal.tella.mobile.util.ThreadUtil
import org.horizontal.tella.mobile.views.activity.MetadataViewerActivity
import org.horizontal.tella.mobile.views.activity.viewer.PermissionsActionsHelper.initContracts
import org.horizontal.tella.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AudioPlayActivity : BaseLockActivity(), StyledPlayerView.ControllerVisibilityListener {
    private var handlingVaultFile: VaultFile? = null
    private var audioPlayer: AudioPlayer? = null
    private var audioPlayerListener: AudioPlayer.Listener? = null
    private var showActions = false
    private var actionsDisabled = false
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
        setContentView(binding.root)
        actionsDisabled = intent.hasExtra(VideoViewerActivity.NO_ACTIONS)

        initListeners()
        initContracts()
        setupToolbar()
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out)
        enablePlay()
        initVaultMediaFile()
        initAudioListener()
        initObservers()
    }

    /**
     * Initializes observers for the AudioPlayActivity.
     */
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
                onMediaFileDeleteConfirmation(
                    mediaFileDeletedConfirmation.vaultFile,
                    mediaFileDeletedConfirmation.showConfirmDelete
                )
            }
            onMediaFileRenamed.observe(this@AudioPlayActivity) { renamed ->
                onMediaFileRename(renamed)
            }
            onMediaFileGot.observe(this@AudioPlayActivity) { renamed ->
                onMediaFileSuccess(renamed)
            }
        }
    }

    /**
     * Initializes the media file for the AudioPlayActivity.
     * Checks if the intent contains the media file directly or its ID.
     * If the media file is available directly, it triggers the onMediaFileSuccess() callback.
     * If only the media file ID is available, it calls the view model to fetch the media file.
     */
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
                this.viewModel.getMediaFile(id)
            }
        }
    }

    /**
     * Initializes the audio player listener for the AudioPlayActivity.
     * This listener provides callbacks for different audio player events such as onStart, onStop, and onProgress.
     */
    private fun initAudioListener() {
        audioPlayerListener = object : AudioPlayer.Listener {
            // Update the duration text when the audio playback starts
            override fun onStart(duration: Int) {
                binding.content.duration.text = timeToString(duration.toLong())
            }

            // Stop the player, update UI, and set the paused flag to true
            override fun onStop() {
                stopPlayer()
                paused = true
                enablePlay()
                showTimeRemaining(0)
            }

            // Update the time remaining text during audio playback progress
            override fun onProgress(currentPosition: Int) {
                showTimeRemaining(currentPosition)
            }

            // Update the time remaining text with the current playback progress
            private fun showTimeRemaining(left: Int) {
                binding.content.audioTime.text = timeToString(left.toLong())
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
        binding.content.playAudio.setOnClickListener {
            if (paused) {
                handlePlay()
            } else {
                handlePause()
            }
        }
        binding.content.fwdButton.setOnClickListener {
            audioPlayer?.ffwd(
                SEEK_DELAY
            )
        }
        binding.content.rwdButton.setOnClickListener {
            audioPlayer?.rwd(
                SEEK_DELAY
            )
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (isInfoShown) {
            toolbar.menu.findItem(R.id.menu_item_more).isVisible = true
            toolbar.menu.findItem(R.id.menu_item_metadata).isVisible = true
            toolbar.title = handlingVaultFile?.name
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
        DialogUtils.showBottomMessage(
            this,
            resources.getQuantityString(R.plurals.gallery_toast_files_exported, 1, 1),
            false
        )
    }

    fun onExportError(error: Throwable) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.gallery_toast_fail_exporting_to_device),
            true
        )
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

    /**
     * Sets up the toolbar for the video player activity.
     */
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

    private fun onMediaFileDeleted() {
        MyApplication.bus().post(MediaFileDeletedEvent())
        finish()
    }

    fun onMediaFileDeletionError(throwable: Throwable) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.gallery_toast_fail_deleting_files),
            true
        )
    }

    private fun onMediaFileRename(vaultFile: VaultFile) {
        toolbar.title = vaultFile.name
        MyApplication.bus().post(VaultFileRenameEvent())
    }


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
            audioPlayer?.play(handlingVaultFile)
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

    /**
     * Disables the play functionality in the video player.
     */
    private fun disablePlay() {
        // Set the play button to display the pause icon.
        binding.content.playAudio.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.big_white_pause_24p,
                theme
            )
        )

        // Enable the forward and rewind buttons.
        enableButton(binding.content.fwdButton, binding.content.rwdButton)
        enableButton(binding.content.rwdButton, binding.content.rwdButton)
    }

    private fun enablePlay() {
        binding.content.playAudio.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_play_arrow_white_24dp,
                theme
            )
        )
        disableButton(binding.content.fwdButton, binding.content.rwdButton)
        disableButton(binding.content.rwdButton, binding.content.rwdButton)
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
            // audioPlayer = null
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

}