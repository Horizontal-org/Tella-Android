package rs.readahead.washington.mobile.views.activity.viewer

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.RadioOptionConsumer
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showRadioListOptionsSheet
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.IVaultActions
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultActionsSheet
import org.hzontal.shared_ui.bottomsheet.VaultSheetUtils.showVaultRenameSheet
import org.hzontal.shared_ui.utils.DialogUtils
import permissions.dispatcher.*
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.databinding.ActivityAudioPlayBinding
import rs.readahead.washington.mobile.media.AudioPlayer
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.util.ThreadUtil
import rs.readahead.washington.mobile.views.activity.MetadataViewerActivity
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

@RuntimePermissions
class AudioPlayActivity : BaseLockActivity() {
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
    private var toolbar: Toolbar? = null
    private var isInfoShown = false
    private lateinit var binding: ActivityAudioPlayBinding
    private val viewModel: SharedMediaFileViewModel by viewModels()
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

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
        initView()
        initListeners()
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out)
        setSupportActionBar(toolbar)
        enablePlay()
        if (intent.hasExtra(Companion.NO_ACTIONS)) {
            actionsDisabled = true
        }
        initAudioListener()
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
        initObservers()

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    // Handle the returned URI here
                    if (uri != null) {
                        viewModel.exportNewMediaFile(withMetadata, handlingVaultFile!!, uri)
                    } else {
                        // Handle the case where no URI is selected
                    }
                }
            }
        }

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
            onMediaFileDeleteConfirmed.observe(this@AudioPlayActivity) { mediaFileDeletedConfirmation->
                mediaFileDeletedConfirmation.vaultFile?.let {
                        deletedVaultFile ->
                    onMediaFileDeleteConfirmation(deletedVaultFile,mediaFileDeletedConfirmation.showConfirmDelete) }
            }
            onMediaFileRenamed.observe(this@AudioPlayActivity) { renamed ->
                onMediaFileRename(renamed)
            }
            onMediaFileGot.observe(this@AudioPlayActivity) { renamed ->
                onMediaFileSuccess(renamed)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!actionsDisabled && showActions) {
            toolbar!!.inflateMenu(R.menu.video_view_menu)
            if (handlingVaultFile != null && handlingVaultFile!!.metadata != null) {
                val item = toolbar!!.menu.findItem(R.id.menu_item_metadata)
                item.isVisible = true
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun onShowError(errorResId: Int) {
        DialogUtils.showBottomMessage(
            this, getString(errorResId), true
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        if (id == R.id.menu_item_more) {
            showVaultActionsDialog(handlingVaultFile)
            return true
        }
        if (id == R.id.menu_item_metadata) {
            showMetadata()
            return true
        }
        return super.onOptionsItemSelected(item)
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
            toolbar!!.menu.findItem(R.id.menu_item_more).isVisible = true
            toolbar!!.menu.findItem(R.id.menu_item_metadata).isVisible = true
            toolbar!!.title = handlingVaultFile!!.name
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


    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun exportMediaFile() {
        if (handlingVaultFile != null && viewModel != null) {
            if (handlingVaultFile!!.metadata != null) {
                showExportWithMetadataDialog()
            } else {
                withMetadata = false
                maybeChangeTemporaryTimeout {
                    performFileSearch()
                    Unit
                }
            }
        }
    }

    private fun performFileSearch() {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            filePickerLauncher.launch(intent)
        } else {
            viewModel.exportNewMediaFile(withMetadata, handlingVaultFile!!, null)
        }
        }


    private fun onMediaFileSuccess(vaultFile: VaultFile) {
        handlingVaultFile = vaultFile
        toolbar!!.title = vaultFile.name
        //handlePlay();
        if (!actionsDisabled) {
            showActions = true
            invalidateOptionsMenu()
        }
    }

    fun onMediaFileError(error: Throwable) {
        Timber.d(error, javaClass.name)
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

    fun onMediaFileDeleteConfirmation(vaultFile: VaultFile, showConfirmDelete: Boolean) {
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
        toolbar!!.title = vaultFile.name
        MyApplication.bus().post(VaultFileRenameEvent())
    }

    fun onMediaFileRenameError(throwable: Throwable) {}

    fun getContext(): Context {
        return this
    }

    private fun shareMediaFile() {
        if (handlingVaultFile == null) {
            return
        }
        if (handlingVaultFile!!.metadata != null) {
            showShareWithMetadataDialog()
        } else {
            startShareActivity(false)
        }
    }

    private fun startShareActivity(includeMetadata: Boolean) {
        if (handlingVaultFile == null) {
            return
        }
        MediaFileHandler.startShareActivity(this, handlingVaultFile, includeMetadata)
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

    private fun showVaultActionsDialog(vaultFile: VaultFile?) {
        showVaultActionsSheet(
            supportFragmentManager,
            vaultFile!!.name,
            getString(R.string.Vault_Upload_SheetAction),
            getString(R.string.Vault_Share_SheetAction),
            getString(R.string.Vault_Move_SheetDesc),
            getString(R.string.Vault_Rename_SheetAction),
            getString(R.string.gallery_action_desc_save_to_device),
            getString(R.string.Vault_File_SheetAction),
            getString(R.string.Vault_Delete_SheetAction),
            isDirectory = false,
            isMultipleFiles = false,
            isUploadVisible = false,
            isMoveVisible = false,
            action = object : IVaultActions {
                override fun upload() {}
                override fun share() {
                    maybeChangeTemporaryTimeout {
                        shareMediaFile()
                        Unit
                    }
                }

                override fun move() {}
                override fun rename() {
                    showVaultRenameSheet(
                        supportFragmentManager,
                        getString(R.string.Vault_RenameFile_SheetTitle),
                        getString(R.string.action_cancel),
                        getString(R.string.action_ok),
                        this@AudioPlayActivity,
                        vaultFile.name
                    ) { name: String? ->
                        viewModel.renameVaultFile(vaultFile.id,name)
                        Unit
                    }
                }

                override fun save() {
                    showConfirmSheet(
                        supportFragmentManager,
                        getString(R.string.gallery_save_to_device_dialog_title),
                        getString(R.string.gallery_save_to_device_dialog_expl),
                        getString(R.string.action_save),
                        getString(R.string.action_cancel),
                        object : BottomSheetUtils.ActionConfirmed {
                            override fun accept(isConfirmed: Boolean) {
                                this@AudioPlayActivity.exportMediaFile()
                            }
                        }
                    )
                }

                override fun info() {
                    toolbar!!.title = getString(R.string.Vault_FileInfo)
                    toolbar!!.menu.findItem(R.id.menu_item_more).isVisible = false
                    toolbar!!.menu.findItem(R.id.menu_item_metadata).isVisible = false
                    invalidateOptionsMenu()
                    addFragment(VaultInfoFragment().newInstance(vaultFile, false), R.id.root)
                    isInfoShown = true
                }

                override fun delete() {
                    showConfirmSheet(
                        supportFragmentManager,
                        getString(R.string.Vault_DeleteFile_SheetTitle),
                        getString(R.string.Vault_deleteFile_SheetDesc),
                        getString(R.string.action_delete),
                        getString(R.string.action_cancel),
                        object : ActionConfirmed {
                            override fun accept(isConfirmed: Boolean) {
                                if (isConfirmed) {
                                    viewModel.confirmDeleteMediaFile(vaultFile)
                                }
                            }
                        }
                    )
                }
            }
        )
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICKER_FILE_REQUEST_CODE) {
//            assert(data != null)
//            viewModel!!.exportNewMediaFile(withMetadata, handlingVaultFile!!, data!!.data)
//        }
//    }

    private fun showShareWithMetadataDialog() {
        val options = LinkedHashMap<Int, Int>()
        options[1] = R.string.verification_share_select_media_and_verification
        options[0] = R.string.verification_share_select_only_media
        showRadioListOptionsSheet(
            supportFragmentManager,
            this,
            options,
            getString(R.string.verification_share_dialog_title),
            getString(R.string.verification_share_dialog_expl),
            getString(R.string.action_ok),
            getString(R.string.action_cancel),
            object : RadioOptionConsumer {
                override fun accept(option: Int) {
                    startShareActivity(option > 0)
                }
            }
        )
    }

    private fun showExportWithMetadataDialog() {
        val options = LinkedHashMap<Int, Int>()
        options[1] = R.string.verification_share_select_media_and_verification
        options[0] = R.string.verification_share_select_only_media
        Handler().post {
            showRadioListOptionsSheet(
                supportFragmentManager,
                this,
                options,
                getString(R.string.verification_share_dialog_title),
                getString(R.string.verification_share_dialog_expl),
                getString(R.string.action_ok),
                getString(R.string.action_cancel),
                object : RadioOptionConsumer {
                    override fun accept(option: Int) {
                        withMetadata = option > 0
                        maybeChangeTemporaryTimeout {
                            performFileSearch()
                            Unit
                        }
                    }
                }
            )
        }
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