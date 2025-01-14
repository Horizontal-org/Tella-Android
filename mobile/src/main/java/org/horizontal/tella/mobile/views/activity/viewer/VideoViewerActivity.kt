package org.horizontal.tella.mobile.views.activity.viewer

import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.hzontal.tella_vault.Metadata.VIEW_METADATA
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.event.MediaFileDeletedEvent
import org.horizontal.tella.mobile.bus.event.VaultFileRenameEvent
import org.horizontal.tella.mobile.databinding.ActivityVideoViewerBinding
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.media.exo.MediaFileDataSourceFactory
import org.horizontal.tella.mobile.util.DialogsUtil
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.util.show
import org.horizontal.tella.mobile.views.activity.MetadataViewerActivity
import org.horizontal.tella.mobile.views.activity.viewer.PermissionsActionsHelper.initContracts
import org.horizontal.tella.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity


@AndroidEntryPoint
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
        binding.progressBar.show()
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out)
        setContentView(binding.root)
        actionsDisabled = intent.hasExtra(NO_ACTIONS)
        initContracts()
        setupToolbar()
        shouldAutoPlay = true
        clearResumePosition()
        simpleExoPlayerView = binding.playerView
        simpleExoPlayerView.setControllerVisibilityListener(this)
        simpleExoPlayerView.requestFocus()

        initObservers()
    }

    /**
     * Initialize observers for observing changes in the ViewModel.
     */
    private fun initObservers() {
        // Observer for error messages.
        with(viewModel) {
            error.observe(this@VideoViewerActivity) {
                onShowError(it)
            }
            // Observer for media file export status.
            onMediaFileExportStatus.observe(this@VideoViewerActivity) { status ->
                when (status) {
                    MediaFileExportStatus.EXPORT_START -> onExportStarted()
                    MediaFileExportStatus.EXPORT_PROGRESS -> onMediaExported()
                    MediaFileExportStatus.EXPORT_END -> onExportEnded()
                }
            }
            // Observer for media file deletion status.
            onMediaFileDeleted.observe(this@VideoViewerActivity) { deleted ->
                if (deleted) onMediaFileDeleted()
            }
            // Observer for media file renaming status.
            onMediaFileRenamed.observe(this@VideoViewerActivity) { renamed ->
                onMediaFileRename(renamed)
            }
            // Observer for media file deletion confirmation.
            onMediaFileDeleteConfirmed.observe(this@VideoViewerActivity) { mediaFileDeletedConfirmation ->
                mediaFileDeletedConfirmation.vaultFile.let { deletedVaultFile ->
                    onMediaFileDeleteConfirmation(
                        deletedVaultFile,
                        mediaFileDeletedConfirmation.showConfirmDelete
                    )
                }
            }

        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Release the player to avoid overlapping playback.
        releasePlayer()

        // Set the flag to indicate that autoplay should be enabled.
        shouldAutoPlay = true

        // Clear the resume position to start playback from the beginning.
        clearResumePosition()

        // Set the new intent.
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
        binding.progressBar.hide()
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


    private fun onMediaFileRename(vaultFile: VaultFile) {
        toolbar.title = vaultFile.name
        this.vaultFile = vaultFile
        MyApplication.bus().post(VaultFileRenameEvent())
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Show the controls on any key event.
        simpleExoPlayerView.showController()
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event)
    }


    /**
     * Initializes the video player with the provided VaultFile.
     */
    private fun initializePlayer() {
        val vaultFile = intent.getSerializableExtra(VIEW_VIDEO) as? VaultFile ?: return

        // Set the current vault file and update the toolbar title.
        this.vaultFile = vaultFile
        toolbar.title = vaultFile.name

        // Setup the metadata menu item based on the availability of metadata.
        setupMetadataMenuItem(vaultFile.metadata != null)

        // Create a data source factory for the media file.
        val mediaFileDataSourceFactory = MediaFileDataSourceFactory(this, vaultFile, null)

        // Create a media item from the media file's URI.
        val mediaItem = MediaItem.fromUri(MediaFileHandler.getEncryptedUri(this, vaultFile))

        // Create a media source from the media item.
        val mediaSource = ProgressiveMediaSource.Factory(mediaFileDataSourceFactory)
            .createMediaSource(mediaItem)

        // Check if we have a resume position for the player.
        val haveResumePosition = resumeWindow != C.INDEX_UNSET

        // Initialize the player if it doesn't exist, otherwise stop the existing player.
        if (player == null) {
            player = ExoPlayer.Builder(this).build().apply {
                playWhenReady = shouldAutoPlay
                simpleExoPlayerView.player = this
            }
        } else {
            player?.stop()
        }
        // Apply the resume position if available, set the media source, and prepare the player.
        player?.apply {
            if (haveResumePosition) {
                seekTo(resumeWindow, resumePosition)
            }
            setMediaSource(mediaSource, !haveResumePosition)
            prepare()
        }

        player?.addListener(PlayerEventListener(binding.progressBar))

        // Reset the retry source flag.
        needRetrySource = false
    }

    /**
     * Releases the video player, clearing its resources and state.
     */
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

    /**
     * Sets up the toolbar for the video player activity.
     */
    private fun setupToolbar() {
        toolbar = binding.playerToolbar

        // Set up the navigation icon and its onClickListener to handle back navigation.
        toolbar.setNavigationIcon(R.drawable.ic_back_white)
        toolbar.setTitleTextColor(resources.getColor(R.color.wa_white))
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // If actions are not disabled, inflate the menu and set up the menu item click listener.
        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.video_view_menu)
            vaultFile?.let { file ->
                // Set up the metadata menu item based on the availability of metadata.
                setupMetadataMenuItem(file.metadata != null)
            }

            // Set up the menu item click listener for the "more" menu item.
            toolbar.menu.findItem(R.id.menu_item_more).setOnMenuItemClickListener {
                vaultFile?.let { file ->
                    // Show the vault actions dialog with the appropriate parameters.
                    showVaultActionsDialog(
                        file,
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

    /**
     * Listener which hides the loading wheel when the video starts
     */
    private class PlayerEventListener(val loaderWheel: View) : Player.Listener {
        override fun onRenderedFirstFrame() {
            loaderWheel.hide()
        }
    }

}
