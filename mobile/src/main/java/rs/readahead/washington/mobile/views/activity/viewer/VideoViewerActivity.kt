package rs.readahead.washington.mobile.views.activity.viewer

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
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.*
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
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.views.activity.MetadataViewerActivity
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.fragment.vault.attachements.PICKER_FILE_REQUEST_CODE
import rs.readahead.washington.mobile.views.fragment.vault.attachements.WRITE_REQUEST_CODE

class VideoViewerActivity : BaseLockActivity(), StyledPlayerView.ControllerVisibilityListener {
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
    private var alertDialog: AlertDialog? = null
    private var progressDialog: ProgressDialog? = null
    private var isInfoShown = false
    private val viewModel: SharedMediaFileViewModel by viewModels()

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

        actionsDisabled = intent.hasExtra(NO_ACTIONS)

        setupToolbar()
        shouldAutoPlay = true
        clearResumePosition()

        simpleExoPlayerView = findViewById(R.id.player_view)
        simpleExoPlayerView.setControllerVisibilityListener(this)
        simpleExoPlayerView.requestFocus()

        initObservers()
    }

    private fun initObservers() {
        with(viewModel) {
            error.observe(this@VideoViewerActivity) {
                onShowError(it)
            }
            onMediaFileExportStatus.observe(this@VideoViewerActivity) { status ->
                when (status) {
                    MediaFileExportStatus.EXPORT_START -> onExportStarted()
                    MediaFileExportStatus.EXPORT_PROGRESS -> onMediaExported()
                    MediaFileExportStatus.EXPORT_END -> onExportEnded()
                }
            }
            onMediaFileDeleted.observe(this@VideoViewerActivity) { deleted ->
                if (deleted) onMediaFileDeleted()
            }
            onMediaFileRenamed.observe(this@VideoViewerActivity) { renamed ->
                onMediaFileRename(renamed)
            }
        }
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
        alertDialog?.takeIf { it.isShowing }?.dismiss()
        hideProgressDialog()
        super.onDestroy()
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun exportMediaFile() {
        if (vaultFile?.metadata != null && withMetadata) {
            showExportWithMetadataDialog()
        } else {
            performFileSearch()
        }
    }

//    private fun performFileSearch() {
//        if (hasStoragePermissions(this)) {
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
//                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//                intent.addFlags(
//                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
//                )
//                startActivityForResult(intent, PICKER_FILE_REQUEST_CODE)
//            } else {
//                vaultFile?.let { viewModel.exportNewMediaFile(withMetadata, it, null) }
//
//            }
//        } else {
//            requestStoragePermissionss()
//        }
//    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == PICKER_FILE_REQUEST_CODE) {
            assert(result.data != null)
            vaultFile?.let { viewModel.exportNewMediaFile(withMetadata, it, result.data?.data) }
        }
    }

   // File search logic here
    private fun performFileSearch() {
        if (hasStoragePermissions(this)) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                filePickerLauncher.launch(intent)
            } else {
                vaultFile?.let { viewModel.exportNewMediaFile(withMetadata, it, null) }
            }
        } else {
            requestStoragePermissions()
        }
    }


    // Check if the app has storage permissions
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

    fun onMediaExported() {
        DialogUtils.showBottomMessage(
            this, resources.getQuantityString(R.plurals.gallery_toast_files_exported, 1, 1), false
        )
    }

    private fun onShowError(errorResId: Int) {
        DialogUtils.showBottomMessage(
            this, getString(errorResId), true
        )
    }

    fun onExportStarted() {
        progressDialog = DialogsUtil.showProgressDialog(
            this, getString(R.string.gallery_save_to_device_dialog_progress_expl)
        )
    }

    fun onExportEnded() {
        hideProgressDialog()
    }
    /* to be used in other activities */
    fun onMediaFileDeleteConfirmation(mediaFileDeleteConfirmation: MediaFileDeleteConfirmation) {
        if (mediaFileDeleteConfirmation.showConfirmDelete) {
            showConfirmSheet(supportFragmentManager,
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
                })
        } else {
            viewModel.deleteMediaFiles(vaultFile)
        }
    }

    fun onMediaFileDeleted() {
        MyApplication.bus().post(MediaFileDeletedEvent())
        finish()
    }


    private fun onMediaFileRename(vaultFile: VaultFile) {
        toolbar.title = vaultFile.name
        this.vaultFile = vaultFile
        MyApplication.bus().post(VaultFileRenameEvent())
    }


    fun getContext(): Context {
        return this
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Show the controls on any key event.
        simpleExoPlayerView.showController()
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event)
    }

    fun shareMediaFile() {
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
        if (player == null) {
            player = ExoPlayer.Builder(this).build().apply {
                playWhenReady = shouldAutoPlay
            }
            simpleExoPlayerView.player = player
        }
        if ( needRetrySource) {
            initializeMedia()
        }
    }
    private fun initializeMedia(){

        val vaultFile = intent.getSerializableExtra(VIEW_VIDEO) as? VaultFile ?: return

            this.vaultFile = vaultFile
            toolbar.title = vaultFile.name
            setupMetadataMenuItem(vaultFile.metadata != null)

            val mediaFileDataSourceFactory = MediaFileDataSourceFactory(this, vaultFile, null)
            val mediaItem = MediaItem.fromUri(MediaFileHandler.getEncryptedUri(this, vaultFile))
            val mediaSource = ProgressiveMediaSource.Factory(mediaFileDataSourceFactory)
                .createMediaSource(mediaItem)

            val haveResumePosition = resumeWindow != C.INDEX_UNSET
            player?.apply {
                if (haveResumePosition) {
                    seekTo(resumeWindow, resumePosition)
                }
                setMediaSource(mediaSource)
                prepare()
               // prepare(mediaSource, !haveResumePosition, false)
            }
            needRetrySource = false

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


    override fun onVisibilityChanged(visibility: Int) {
        toolbar.visibility = if (!isInfoShown) visibility else View.VISIBLE
    }
    private fun setupToolbar() {
        toolbar = binding.playerToolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.video_view_menu)
            vaultFile?.let { file ->
                setupMetadataMenuItem(file.metadata != null)
            }

            toolbar.menu.findItem(R.id.menu_item_more)
                .setOnMenuItemClickListener {
                    VaultActionsHelper.showVaultActionsDialog(this,vaultFile,viewModel,{
                        isInfoShown = true
                        onVisibilityChanged(View.VISIBLE)
                    }, toolbar = toolbar)
                    false
                }
        }
    }

    private fun showMetadata() {
        val viewMetadata = Intent(this, MetadataViewerActivity::class.java).apply {
            putExtra(VIEW_METADATA, vaultFile)
        }
        startActivity(viewMetadata)
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
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

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICKER_FILE_REQUEST_CODE) {
//            assert(data != null)
//            vaultFile?.let { viewModel.exportNewMediaFile(withMetadata, it, data?.data) }
//        }
//    }

    private fun showShareWithMetadataDialog() {
        val options = mapOf(
            R.string.verification_share_select_media_and_verification to R.string.verification_share_select_media_and_verification,
            R.string.verification_share_select_only_media to R.string.verification_share_select_only_media
        )
        showRadioListOptionsSheet(supportFragmentManager,
            this,
            options as LinkedHashMap<Int, Int>,
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
        val options = mapOf(
            R.string.verification_share_select_media_and_verification to R.string.verification_share_select_media_and_verification,
            R.string.verification_share_select_only_media to R.string.verification_share_select_only_media
        )
        Handler().post {
            showRadioListOptionsSheet(supportFragmentManager,
                this,
                options as LinkedHashMap<Int, Int>,
                getString(R.string.verification_share_dialog_title),
                getString(R.string.verification_share_dialog_expl),
                getString(R.string.action_ok),
                getString(R.string.action_cancel),
                object : RadioOptionConsumer {
                    override fun accept(option: Int) {
                        withMetadata = option > 0
                        maybeChangeTemporaryTimeout {
                            //performFileSearch()
                        }
                    }
                })
        }
    }

//    private fun requestStoragePermissions() {
//        maybeChangeTemporaryTimeout()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                .addCategory(Intent.CATEGORY_DEFAULT)
//                .setData(Uri.parse("package:${application.packageName}"))
//            startActivityForResult(intent, WRITE_REQUEST_CODE)
//        } else {
//            ActivityCompat.requestPermissions(
//                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_REQUEST_CODE
//            )
//        }
//    }

    //    @SuppressLint("NeedOnRequestPermissionsResult")
//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == WRITE_REQUEST_CODE) {
//            // performFileSearch()
//            LockTimeoutManager().lockTimeout = Preferences.getLockTimeout()
//        }
//    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                LockTimeoutManager().lockTimeout = Preferences.getLockTimeout()
                // Permission granted, perform the necessary actions
            } else {
                // Permission denied, handle accordingly
            }
        }
    private fun requestStoragePermissions() {
        maybeChangeTemporaryTimeout()
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }



}