package rs.readahead.washington.mobile.views.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.*
import com.google.android.exoplayer2.upstream.*
import com.hzontal.tella_vault.Metadata.VIEW_METADATA
import com.hzontal.tella_vault.VaultFile
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
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.ActivityVideoViewerBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.media.exo.MediaFileDataSourceFactory
import rs.readahead.washington.mobile.mvp.contract.IMediaFileViewerPresenterContract
import rs.readahead.washington.mobile.mvp.presenter.MediaFileViewerPresenter
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.PICKER_FILE_REQUEST_CODE
import rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers.WRITE_REQUEST_CODE
import rs.readahead.washington.mobile.views.fragment.vault.info.VaultInfoFragment


@RuntimePermissions
class VideoViewerActivity : BaseLockActivity(), StyledPlayerControlView.VisibilityListener,
    IMediaFileViewerPresenterContract.IView {
    private lateinit var simpleExoPlayerView: StyledPlayerView
    private lateinit var binding: ActivityVideoViewerBinding
    private lateinit var toolbar: Toolbar
    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var needRetrySource = false
    private var shouldAutoPlay = false
    private var withMetadata = false
    private var resumeWindow = 0
    private var resumePosition: Long = 0
    private var vaultFile: VaultFile? = null
    private var actionsDisabled = false
    private var presenter: MediaFileViewerPresenter? = null
    private var alertDialog: AlertDialog? = null
    private var progressDialog: ProgressDialog? = null
    private var isInfoShown = false

    companion object {
        const val VIEW_VIDEO = "vv"
        const val NO_ACTIONS = "na"
        val SDK_INT =
            if (Build.VERSION.SDK_INT == 25 && Build.VERSION.CODENAME[0] == 'O') 26 else Build.VERSION.SDK_INT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoViewerBinding.inflate(layoutInflater)

        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out)
        setContentView(binding.root)

        if (intent.hasExtra(NO_ACTIONS)) {
            actionsDisabled = true
        }
        setupToolbar()
        shouldAutoPlay = true
        clearResumePosition()
        simpleExoPlayerView = findViewById(R.id.player_view)
        simpleExoPlayerView.setControllerVisibilityListener(this)
        simpleExoPlayerView.requestFocus()
        presenter = MediaFileViewerPresenter(this)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releasePlayer()
        shouldAutoPlay = true
        clearResumePosition()
        setIntent(intent)
    }

    public override fun onStart() {
        super.onStart()
        if (SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onDestroy() {
        if (alertDialog != null) {
            if (alertDialog!!.isShowing) {
                alertDialog!!.dismiss()
            }
        }
        hideProgressDialog()
        if (presenter != null) {
            presenter!!.destroy()
        }
        super.onDestroy()
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun exportMediaFile() {
        if (vaultFile != null && presenter != null) {
            if (vaultFile?.metadata != null) {
                showExportWithMetadataDialog()
            } else {
                withMetadata = false
                maybeChangeTemporaryTimeout {
                    performFileSearch()
                }
            }
        }
    }

    private fun performFileSearch() {
        if (hasStoragePermissions(this)) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                startActivityForResult(intent, PICKER_FILE_REQUEST_CODE)
            } else {
                if (presenter != null) {
                    presenter!!.exportNewMediaFile(withMetadata, vaultFile, null)
                }
            }
        } else {
            requestStoragePermissions()
        }
    }

    private fun hasStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result: Int = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val result1: Int = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onMediaExported() {
        DialogUtils.showBottomMessage(
            this,
            resources.getQuantityString(R.plurals.gallery_toast_files_exported, 1, 1),
            false
        )
    }

    override fun onExportError(error: Throwable) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.gallery_toast_fail_exporting_to_device),
            true
        )
    }

    override fun onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(
            this, getString(R.string.gallery_save_to_device_dialog_progress_expl)
        )
    }

    override fun onExportEnded() {
        hideProgressDialog()
    }

    override fun onMediaFileDeleteConfirmation(vaultFile: VaultFile, showConfirmDelete: Boolean) {
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
                            presenter?.deleteMediaFiles(vaultFile)
                        }
                    }
                }
            )
        } else {
            presenter?.deleteMediaFiles(vaultFile)
        }
    }

    override fun onMediaFileDeleted() {
        MyApplication.bus().post(MediaFileDeletedEvent())
        finish()
    }

    override fun onMediaFileDeletionError(throwable: Throwable) {
        DialogUtils.showBottomMessage(
            this,
            getString(R.string.gallery_toast_fail_deleting_files),
            true
        )
    }

    override fun onMediaFileRename(vaultFile: VaultFile) {
        toolbar.title = vaultFile.name
        this.vaultFile = vaultFile
        MyApplication.bus().post(VaultFileRenameEvent())
    }

    override fun onMediaFileRenameError(throwable: Throwable) {
        //TODO CHECK ERROR MSG WHEN RENAME
        DialogUtils.showBottomMessage(
            this, getString(R.string.gallery_toast_fail_deleting_files), true
        )
    }

    override fun getContext(): Context {
        return this
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Show the controls on any key event.
        simpleExoPlayerView.showController()
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event)
    }

    private fun shareMediaFile() {
        if (vaultFile == null) {
            return
        }
        if (vaultFile?.metadata != null) {
            showShareWithMetadataDialog()
        } else {
            startShareActivity(false)
        }
    }

    private fun startShareActivity(includeMetadata: Boolean) {
        if (vaultFile == null) {
            return
        }
        MediaFileHandler.startShareActivity(this, vaultFile, includeMetadata)
    }

    private fun initializePlayer() {
        val needNewPlayer = player == null
        if (needNewPlayer) {
            player = ExoPlayer.Builder(this).build()
            player?.apply {
                binding.playerView.player
                playWhenReady = shouldAutoPlay
            }

            simpleExoPlayerView.setPlayer(player)
        }
        if (needNewPlayer || needRetrySource) {
            if (intent.hasExtra(VIEW_VIDEO) && intent.extras != null) {
                val vaultFile = intent.extras!![VIEW_VIDEO] as VaultFile?
                if (vaultFile != null) {
                    this.vaultFile = vaultFile
                    toolbar.title = vaultFile.name
                    setupMetadataMenuItem(vaultFile.metadata != null)
                    val mediaFileDataSourceFactory = MediaFileDataSourceFactory(
                        this, vaultFile, null
                    )
                    val mediaItem =
                        MediaItem.fromUri(MediaFileHandler.getEncryptedUri(this, vaultFile))
                    val mediaSource: MediaSource =
                        ProgressiveMediaSource.Factory(mediaFileDataSourceFactory)
                            .createMediaSource(mediaItem)

                    val haveResumePosition = resumeWindow != C.INDEX_UNSET
                    if (player != null) {
                        if (haveResumePosition) {
                            player?.seekTo(resumeWindow, resumePosition)
                        }
                        player?.prepare(mediaSource, !haveResumePosition, false)
                    }
                    needRetrySource = false
                }
            }
        }
    }

    private fun releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player?.playWhenReady == true
            //updateResumePosition(); // todo: fix source skipping..
            player?.release()
            player = null
            trackSelector = null
            clearResumePosition()
        }
    }

    private fun clearResumePosition() {
        resumeWindow = C.INDEX_UNSET
        resumePosition = C.TIME_UNSET
    }

    override fun onVisibilityChange(visibility: Int) {
        if (!isInfoShown) {
            toolbar.visibility = visibility
        } else {
            toolbar.show()
        }
    }

    private fun setupToolbar() {
        toolbar = binding.playerToolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener({ v: View? -> onBackPressed() })
        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.video_view_menu)
            if (vaultFile != null) {
                setupMetadataMenuItem(vaultFile!!.metadata != null)
            }
            toolbar.getMenu().findItem(R.id.menu_item_more)
                .setOnMenuItemClickListener { item: MenuItem? ->
                    showVaultActionsDialog(vaultFile)
                    false
                }
        }
    }

    private fun showMetadata() {
        val viewMetadata = Intent(
            this, MetadataViewerActivity::class.java
        )
        viewMetadata.putExtra(VIEW_METADATA, vaultFile)
        startActivity(viewMetadata)
    }

    private fun hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    private fun setupMetadataMenuItem(visible: Boolean) {
        if (actionsDisabled) {
            return
        }
        val mdMenuItem = toolbar.menu.findItem(R.id.menu_item_metadata)
        if (visible) {
            mdMenuItem.setVisible(true).setOnMenuItemClickListener { item: MenuItem? ->
                showMetadata()
                false
            }
        } else {
            mdMenuItem.isVisible = false
        }
    }

    private fun showVaultActionsDialog(vaultFile: VaultFile?) {
        showVaultActionsSheet(supportFragmentManager,
            vaultFile?.name,
            getString(R.string.Vault_Upload_SheetAction),
            getString(R.string.Vault_Share_SheetAction),
            getString(R.string.Vault_Move_SheetDesc),
            getString(R.string.Vault_Rename_SheetAction),
            getString(R.string.gallery_action_desc_save_to_device),
            getString(R.string.Vault_File_SheetAction),
            getString(R.string.Vault_Delete_SheetAction),
            false,
            false,
            false,
            false,
            object : IVaultActions {
                override fun upload() {}
                override fun share() {
                    maybeChangeTemporaryTimeout {
                        shareMediaFile()
                    }
                }

                override fun move() {}
                override fun rename() {
                    showVaultRenameSheet(
                        supportFragmentManager,
                        getString(R.string.Vault_CreateFolder_SheetAction),
                        getString(R.string.action_cancel),
                        getString(R.string.action_ok),
                        this@VideoViewerActivity,
                        vaultFile?.name
                    ) { name: String? ->
                        presenter?.renameVaultFile(vaultFile?.id, name)
                    }
                }

                override fun save() {
                    showConfirmSheet(supportFragmentManager,
                        getString(R.string.gallery_save_to_device_dialog_title),
                        getString(R.string.gallery_save_to_device_dialog_expl),
                        getString(R.string.action_save),
                        getString(R.string.action_cancel),
                        object : ActionConfirmed {
                            override fun accept(isConfirmed: Boolean) {
                                exportMediaFile()
                            }
                        })
                }

                override fun info() {
                    isInfoShown = true
                    onVisibilityChange(View.VISIBLE)
                    toolbar.title = getString(R.string.Vault_FileInfo)
                    toolbar.menu.findItem(R.id.menu_item_more).isVisible = false
                    toolbar.menu.findItem(R.id.menu_item_metadata).isVisible = false
                    invalidateOptionsMenu()
                    vaultFile?.let {  VaultInfoFragment.newInstance(it, false) }
                        ?.let { addFragment(it, R.id.container) }
                }

                override fun delete() {
                    showConfirmSheet(supportFragmentManager,
                        getString(R.string.Vault_DeleteFile_SheetTitle),
                        getString(R.string.Vault_deleteFile_SheetDesc),
                        getString(R.string.action_delete),
                        getString(R.string.action_cancel),
                        object : ActionConfirmed {
                            override fun accept(isConfirmed: Boolean) {
                                presenter?.deleteMediaFiles(
                                    vaultFile
                                )
                            }
                        })
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICKER_FILE_REQUEST_CODE) {
            assert(data != null)
            presenter?.exportNewMediaFile(withMetadata, vaultFile, data?.data)
        }
    }

    private fun showShareWithMetadataDialog() {
        val options = LinkedHashMap<Int, Int>()
        options[1] = R.string.verification_share_select_media_and_verification
        options[0] = R.string.verification_share_select_only_media
        showRadioListOptionsSheet(supportFragmentManager,
            context,
            options,
            getString(R.string.verification_share_dialog_title),
            getString(R.string.verification_share_dialog_expl),
            getString(R.string.action_ok),
            getString(R.string.action_cancel),
            object : RadioOptionConsumer {
                override fun accept(option: Int) {
                    startShareActivity(option > 0)
                }
            })
    }

    private fun showExportWithMetadataDialog() {
        val options = LinkedHashMap<Int, Int>()
        options[1] = R.string.verification_share_select_media_and_verification
        options[0] = R.string.verification_share_select_only_media
        Handler().post {
            showRadioListOptionsSheet(supportFragmentManager,
                context,
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
                        }
                    }
                })
        }
    }

    private fun requestStoragePermissions() {
        maybeChangeTemporaryTimeout()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(
                    String.format(
                        "package:%s", application.packageName
                    )
                )
                startActivityForResult(intent, WRITE_REQUEST_CODE)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, WRITE_REQUEST_CODE)
            }
        } else {
            //below android 11
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_REQUEST_CODE
            )
        }
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_REQUEST_CODE) {
            performFileSearch()
            LockTimeoutManager().lockTimeout = Preferences.getLockTimeout()
        }
    }

}