package rs.readahead.washington.mobile.views.activity.viewer

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
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
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import permissions.dispatcher.RuntimePermissions
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.databinding.ActivityPhotoViewerBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.views.activity.MetadataViewerActivity
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.initContracts
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity


@RuntimePermissions
class PhotoViewerActivity : BaseLockActivity(), StyledPlayerView.ControllerVisibilityListener {
    companion object {
        const val VIEW_PHOTO = "vp"
        const val NO_ACTIONS = "na"
    }

    private val viewModel: SharedMediaFileViewModel by viewModels()
    private lateinit var toolbar: Toolbar
    private var vaultFile: VaultFile? = null
    private var showActions = false
    private var actionsDisabled = false
    private var alertDialog: AlertDialog? = null
    private var isInfoShown = false
    private lateinit var binding: ActivityPhotoViewerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initContracts()
        setupToolbar()
        initObservers()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.toolbar.outlineProvider = null
        } else {
            binding.toolbar.bringToFront()
        }
        overridePendingTransition(R.anim.slide_in_start, R.anim.fade_out)
        initVaultMediaFile()
        openMedia()
    }

    private fun initVaultMediaFile() {
        if (intent.hasExtra(VIEW_PHOTO)) {
            val vaultFile = intent.getSerializableExtra(VIEW_PHOTO) as VaultFile?
            if (vaultFile != null) {
                this.vaultFile = vaultFile
            }
        }
        toolbar.title = vaultFile!!.name
        if (intent.hasExtra(NO_ACTIONS)) {
            actionsDisabled = true
        }
    }

    override fun onVisibilityChanged(visibility: Int) {
        binding.toolbar.visibility = if (!isInfoShown) visibility else View.VISIBLE
    }


    private fun initObservers() {
        with(viewModel) {
            error.observe(this@PhotoViewerActivity) {
                 onShowError(it)
            }
            onMediaFileExportStatus.observe(this@PhotoViewerActivity) { status ->
                when (status) {
                    MediaFileExportStatus.EXPORT_START -> onExportStarted()
                    MediaFileExportStatus.EXPORT_PROGRESS -> onMediaExported()
                    MediaFileExportStatus.EXPORT_END -> onExportEnded()
                }
            }
            onMediaFileDeleted.observe(this@PhotoViewerActivity) { deleted ->
                if (deleted) onMediaFileDeleted()
            }
            onMediaFileRenamed.observe(this@PhotoViewerActivity) { renamed ->
                onMediaFileRename(renamed)
            }

            onMediaFileDeleteConfirmed.observe(this@PhotoViewerActivity) { mediaFileDeletedConfirmation ->
                mediaFileDeletedConfirmation.vaultFile?.let { deletedVaultFile ->
                    onMediaFileDeleteConfirmation(
                        deletedVaultFile,
                        mediaFileDeletedConfirmation.showConfirmDelete
                    )
                }
            }
        }
    }

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

    private fun onShowError(errorResId: Int) {
        DialogUtils.showBottomMessage(
            this, getString(errorResId), true
        )
    }
    private fun setupToolbar() {
        toolbar = binding.toolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        if (!actionsDisabled) {
            toolbar.inflateMenu(R.menu.photo_view_menu)
            vaultFile?.let { file ->
                setupMetadataMenuItem(file.metadata != null)
            }
            if (vaultFile != null && vaultFile!!.metadata != null) {
                val item = toolbar.menu.findItem(R.id.menu_item_metadata)
                item.isVisible = true
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

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        if (!actionsDisabled && showActions) {
//            menuInflater.inflate(R.menu.photo_view_menu, menu)
//            if (vaultFile!!.metadata != null) {
//                val item = menu.findItem(R.id.menu_item_metadata)
//                item.isVisible = true
//            }
//        }
//        this.menu = menu
//        return super.onCreateOptionsMenu(menu)
//    }
//
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val id = item.itemId
//        if (id == android.R.id.home) {
//            onBackPressed()
//            return true
//        }
//        if (id == R.id.menu_item_more) {
//            vaultFile?.let { it1 ->
//                showVaultActionsDialog(
//                    it1,
//                    viewModel,
//                    {
//                        isInfoShown = true
//                        onVisibilityChanged(View.VISIBLE)
//                    },
//                    toolbar = binding!!.toolbar
//                )
//            }
//            return true
//        }
//        if (id == R.id.menu_item_metadata) {
//            showMetadata()
//            return true
//        }
//        return super.onOptionsItemSelected(item)
//    }

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

//    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//    fun showWriteExternalStorageRationale(request: PermissionRequest?) {
//        maybeChangeTemporaryTimeout()
//        alertDialog = showRationale(
//            this,
//            request!!,
//            getString(R.string.permission_dialog_expl_device_storage)
//        )
//    }


    private fun openMedia() {
        showGalleryImage(vaultFile)
        if (!actionsDisabled) {
            showActions = true
            invalidateOptionsMenu()
        }
    }

    fun onMediaExported() {
        showToast(resources.getQuantityString(R.plurals.gallery_toast_files_exported, 1, 1))
    }

    fun onExportStarted() {
        binding.content.progressBar.visibility = View.VISIBLE
    }

    fun onExportEnded() {
        binding.content.progressBar.visibility = View.GONE
    }

    fun onMediaFileDeleted() {
        MyApplication.bus().post(MediaFileDeletedEvent())
        finish()
    }

    private fun onMediaFileRename(vaultFile: VaultFile) {
        toolbar.title = vaultFile.name
        MyApplication.bus().post(VaultFileRenameEvent())
    }

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
                    binding!!.content.progressBar.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any,
                    target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding!!.content.progressBar.visibility = View.GONE
                    return false
                }
            })
            .into(binding!!.content.photoImageView)
    }

    private fun showMetadata() {
        val viewMetadata = Intent(this, MetadataViewerActivity::class.java)
        viewMetadata.putExtra(Metadata.VIEW_METADATA, vaultFile)
        startActivity(viewMetadata)
    }


//    private fun showVaultActionsDialog(vaultFile: VaultFile?) {
//        showVaultActionsSheet(
//            supportFragmentManager,
//            vaultFile!!.name,
//            getString(R.string.Vault_Upload_SheetAction),
//            getString(R.string.Vault_Share_SheetAction),
//            getString(R.string.Vault_Move_SheetDesc),
//            getString(R.string.Vault_Rename_SheetAction),
//            getString(R.string.gallery_action_desc_save_to_device),
//            getString(R.string.Vault_File_SheetAction),
//            getString(R.string.Vault_Delete_SheetAction),
//            false,
//            false,
//            false,
//            false,
//            object : IVaultActions {
//                override fun upload() {}
//                override fun share() {
//                    maybeChangeTemporaryTimeout {
//                        shareMediaFile()
//                        Unit
//                    }
//                }
//
//                override fun move() {}
//                override fun rename() {
//                    showVaultRenameSheet(
//                        supportFragmentManager,
//                        getString(R.string.Vault_CreateFolder_SheetAction),
//                        getString(R.string.action_cancel),
//                        getString(R.string.action_ok),
//                        this@PhotoViewerActivity,
//                        vaultFile.name
//                    ) { name: String? ->
//                      //  presenter!!.renameVaultFile(vaultFile.id, name)
//                        Unit
//                    }
//                }
//
//                override fun save() {
//                    showConfirmSheet(
//                        supportFragmentManager,
//                        getString(R.string.gallery_save_to_device_dialog_title),
//                        getString(R.string.gallery_save_to_device_dialog_expl),
//                        getString(R.string.action_save),
//                        getString(R.string.action_cancel),
//                        object : ActionConfirmed {
//                            override fun accept(isConfirmed: Boolean) {
//
//                            }
//                        }
//                    )
//                }
//
//                override fun info() {
//                    binding!!.toolbar.title = getString(R.string.Vault_FileInfo)
//                    menu!!.findItem(R.id.menu_item_more).isVisible = false
//                    menu!!.findItem(R.id.menu_item_metadata).isVisible = false
//                    invalidateOptionsMenu()
//                    addFragment(
//                        VaultInfoFragment().newInstance(vaultFile, false),
//                        R.id.photo_viewer_container
//                    )
//                }
//
//                override fun delete() {
//                    showConfirmSheet(
//                        supportFragmentManager,
//                        getString(R.string.Vault_DeleteFile_SheetTitle),
//                        getString(R.string.Vault_deleteFile_SheetDesc),
//                        getString(R.string.action_delete),
//                        getString(R.string.action_cancel),
//                        object : ActionConfirmed {
//                            override fun accept(isConfirmed: Boolean) {
//                                if (isConfirmed) {
//                                    //presenter!!.confirmDeleteMediaFile(vaultFile)
//                                }
//                            }
//                        }
//                    )
//                }
//            }
//        )
//    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICKER_FILE_REQUEST_CODE) {
//            assert(data != null)
//            //presenter!!.exportNewMediaFile(withMetadata, vaultFile, data!!.data)
//        }
//    }


}