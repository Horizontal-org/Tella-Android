package rs.readahead.washington.mobile.views.activity.viewer

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.hzontal.tella_vault.Metadata.VIEW_METADATA
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.databinding.ActivityVideoViewerBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.media.exo.MediaFileDataSourceFactory
import rs.readahead.washington.mobile.util.DialogsUtil
import rs.readahead.washington.mobile.views.activity.MetadataViewerActivity
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.initContracts
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity

class VideoViewerActivity : BaseLockActivity(), StyledPlayerView.ControllerVisibilityListener {
    private lateinit var simpleExoPlayerView: StyledPlayerView
    private lateinit var binding: ActivityVideoViewerBinding
    private lateinit var toolbar: Toolbar
    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var needRetrySource = false
    private var shouldAutoPlay = false
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
        initContracts()
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


    private fun startShareActivity(includeMetadata: Boolean) {
        if (vaultFile == null) {
            return
        }
        MediaFileHandler.startShareActivity(this, vaultFile, includeMetadata)
    }

    private fun initializePlayer() {
        val vaultFile = intent.getSerializableExtra(VIEW_VIDEO) as? VaultFile ?: return

        this.vaultFile = vaultFile
        toolbar.title = vaultFile.name
        setupMetadataMenuItem(vaultFile.metadata != null)

        val mediaFileDataSourceFactory = MediaFileDataSourceFactory(this, vaultFile, null)
        val mediaItem = MediaItem.fromUri(MediaFileHandler.getEncryptedUri(this, vaultFile))
        val mediaSource = ProgressiveMediaSource.Factory(mediaFileDataSourceFactory)
            .createMediaSource(mediaItem)

        val haveResumePosition = resumeWindow != C.INDEX_UNSET
        if (player == null) {
            player = SimpleExoPlayer.Builder(this).build().apply {
                playWhenReady = shouldAutoPlay
                simpleExoPlayerView.player = this
            }
        } else {
            player?.stop()
        }

        player?.apply {
            if (haveResumePosition) {
                seekTo(resumeWindow, resumePosition)
            }
            setMediaSource(mediaSource, !haveResumePosition)
            prepare()
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
                    vaultFile?.let { it1 ->
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

}