package org.horizontal.tella.mobile.views.activity.viewer

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.event.MediaFileDeletedEvent
import org.horizontal.tella.mobile.bus.event.VaultFileRenameEvent
import org.horizontal.tella.mobile.databinding.ActivityPhotoViewerBinding
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.util.configureAppBar
import org.horizontal.tella.mobile.views.activity.MetadataViewerActivity
import org.horizontal.tella.mobile.views.activity.viewer.PermissionsActionsHelper.initContracts
import org.horizontal.tella.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.horizontal.tella.mobile.views.fragment.vault.edit.VaultEditFragment

@AndroidEntryPoint
class PhotoViewerActivity : BaseLockActivity(), StyledPlayerView.ControllerVisibilityListener {
    companion object {
        const val VIEW_PHOTO = "vp"
        const val NO_ACTIONS = "na"
        const val CURRENT_EDIT_PARENT = "cep"
    }

    private val viewModel: SharedMediaFileViewModel by viewModels()
    private lateinit var toolbar: Toolbar
    private var vaultFile: VaultFile? = null
    private var showActions = false
    private var actionsDisabled = false
    private var alertDialog: AlertDialog? = null
    private var isInfoShown = false
    private lateinit var binding: ActivityPhotoViewerBinding
    private var currentParent: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initVaultMediaFile()
        initContracts()
        initObservers()

        binding.toolbar.configureAppBar()
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out)
        openMedia()
        setupToolbar()
    }

    /**
     * Initializes the VaultMediaFile activity by retrieving the VaultFile from the intent extras.
     * and checks if actions are disabled.
     */
    private fun initVaultMediaFile() {
        if (intent.hasExtra(VIEW_PHOTO)) {
            val vaultFile = intent.getSerializableExtra(VIEW_PHOTO) as VaultFile?
            if (vaultFile != null) {
                this.vaultFile = vaultFile
            }
        }
        if (intent.hasExtra(NO_ACTIONS)) {
            actionsDisabled = true
        }
        if (intent.hasExtra(CURRENT_EDIT_PARENT)) {
            currentParent = intent.getStringExtra(CURRENT_EDIT_PARENT)
        }
    }

    override fun onVisibilityChanged(visibility: Int) {
        binding.toolbar.visibility = if (!isInfoShown) visibility else View.VISIBLE
    }

    /**
     * Initializes observers for LiveData in the ViewModel and sets up corresponding actions when the LiveData values change.
     */
    private fun initObservers() {
        with(viewModel) {
            // Observer for the error LiveData, displays the error message when it is triggered
            error.observe(this@PhotoViewerActivity) {
                onShowError(it)
            }
            // Observer for the onMediaFileExportStatus LiveData, handles different export status cases
            onMediaFileExportStatus.observe(this@PhotoViewerActivity) { status ->
                when (status) {
                    MediaFileExportStatus.EXPORT_START -> onExportStarted()
                    MediaFileExportStatus.EXPORT_PROGRESS -> onMediaExported()
                    MediaFileExportStatus.EXPORT_END -> onExportEnded()
                }
            }

            // Observer for the onMediaFileDeleted LiveData, handles the action when a media file is deleted
            onMediaFileDeleted.observe(this@PhotoViewerActivity) { deleted ->
                if (deleted) onMediaFileDeleted()
            }

            // Observer for the onMediaFileRenamed LiveData, handles the action when a media file is renamed
            onMediaFileRenamed.observe(this@PhotoViewerActivity) { renamed ->
                vaultFile?.name = renamed.name
                onMediaFileRename(renamed)
            }

            // Observer for the onMediaFileDeleteConfirmed LiveData, handles the action when media file deletion is confirmed
            onMediaFileDeleteConfirmed.observe(this@PhotoViewerActivity) { mediaFileDeletedConfirmation ->
                onMediaFileDeleteConfirmation(
                    mediaFileDeletedConfirmation.vaultFile,
                    mediaFileDeletedConfirmation.showConfirmDelete
                )
            }
        }
    }

    /**
     * Handles the action when media file deletion is confirmed.
     * If showConfirmDelete is true, shows a confirmation bottom sheet to confirm the deletion,
     * otherwise, directly initiates the deletion of the media file.
     *
     * @param vaultFile The VaultFile to be deleted.
     * @param showConfirmDelete Flag indicating whether to show a confirmation bottom sheet or not.
     */
    private fun onMediaFileDeleteConfirmation(vaultFile: VaultFile, showConfirmDelete: Boolean) {
        if (showConfirmDelete) {
            BottomSheetUtils.showConfirmSheet(
                supportFragmentManager,
                getString(R.string.Vault_Warning_Title),
                getString(R.string.Vault_Confirm_delete_Description),
                getString(R.string.Vault_Delete_anyway),
                getString(R.string.action_cancel),
                object : BottomSheetUtils.ActionConfirmed {
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

    /**
     * Displays a bottom message dialog to show the specified error message.
     *
     * @param errorResId The resource ID of the error message to be shown.
     */
    private fun onShowError(errorResId: Int) {
        DialogUtils.showBottomMessage(
            this, getString(errorResId), true
        )
    }

    /**
     * Sets up the toolbar for the activity, including navigation icon, title, menu items, and click listeners.
     * If actions are not disabled, it inflates the menu with actions like metadata and more options.
     */
    private fun setupToolbar() {
        toolbar = binding.toolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        toolbar.title = vaultFile!!.name
        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.photo_view_menu)
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
            toolbar.menu.findItem(R.id.menu_item_edit)
                .setOnMenuItemClickListener {
                    vaultFile?.let { file ->
                        toolbar.visibility = View.GONE
                        addFragment(
                            vaultFile.let {
                                VaultEditFragment.newInstance(
                                    file,
                                    false,
                                    currentParent
                                )
                            },
                            R.id.container
                        )
                    }
                    false
                }
        }
    }

    /**
     * Sets up the metadata menu item in the toolbar based on the visibility status.
     * If actions are disabled, the method returns without doing anything.
     * If the metadata is visible, it shows the menu item and sets a click listener to show the metadata.
     * @param visible Boolean indicating whether the metadata menu item should be visible or not.
     */
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


    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_end, R.anim.slide_out_start)
    }

    override fun onDestroy() {
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog!!.dismiss()
        }
        super.onDestroy()
    }

    private fun openMedia() {
        showGalleryImage(vaultFile)
        if (!actionsDisabled) {
            showActions = true
            invalidateOptionsMenu()
        }
    }

    private fun onMediaExported() {
        DialogUtils.showBottomMessage(
            this,
            resources.getQuantityString(R.plurals.gallery_toast_files_exported, 1, 1),
            false
        )
    }

    private fun onExportStarted() {
        binding.content.progressBar.visibility = View.VISIBLE
    }

    private fun onExportEnded() {
        binding.content.progressBar.visibility = View.GONE
    }

    private fun onMediaFileDeleted() {
        MyApplication.bus().post(MediaFileDeletedEvent())
        finish()
    }

    private fun onMediaFileRename(vaultFile: VaultFile) {
        toolbar.title = vaultFile.name
        MyApplication.bus().post(VaultFileRenameEvent())
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        if (toolbar.menu.findItem(R.id.menu_item_more) != null) {
            toolbar.menu.findItem(R.id.menu_item_more).isVisible = true
        }
        if (vaultFile!!.metadata != null && toolbar.menu.findItem(R.id.menu_item_metadata) != null) {
            toolbar.menu.findItem(R.id.menu_item_metadata).isVisible = true
        }
        binding.toolbar.title = vaultFile!!.name
        finish()
    }

    /**
     * Displays the image from the gallery using Glide library.
     * @param vaultFile The VaultFile containing the image to be displayed.
     */
    private fun showGalleryImage(vaultFile: VaultFile?) {
        val uri = MediaFileHandler.getEncryptedUri(this, vaultFile)
        Glide.with(this)
            .load(uri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.content.progressBar.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any,
                    target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.content.progressBar.visibility = View.GONE
                    return false
                }
            })
            .into(binding.content.photoImageView)
    }

    private fun showMetadata() {
        val viewMetadata = Intent(this, MetadataViewerActivity::class.java)
        viewMetadata.putExtra(Metadata.VIEW_METADATA, vaultFile)
        startActivity(viewMetadata)
    }


}