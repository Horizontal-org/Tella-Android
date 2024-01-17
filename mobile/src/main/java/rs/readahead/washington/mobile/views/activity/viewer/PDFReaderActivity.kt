package rs.readahead.washington.mobile.views.activity.viewer

import android.os.Bundle
import androidx.activity.viewModels
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.databinding.ActivityPdfReaderBinding
import rs.readahead.washington.mobile.views.activity.viewer.PermissionsActionsHelper.initContracts
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity

@AndroidEntryPoint
class PDFReaderActivity : BaseLockActivity() {
    private val viewModel: SharedMediaFileViewModel by viewModels()
    private lateinit var binding: ActivityPdfReaderBinding
    private var vaultFile: VaultFile? = null
    private var actionsDisabled = false
    private var pageNumber = 0
    private var pdfFileName: String? = null
    private var isInfoShown = false

    companion object {
        const val VIEW_PDF = "vp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
      //  initVaultMediaFile()
       // initObservers()
        initContracts()
      //  setupToolbar()
    }

    /*

    private fun initVaultMediaFile() {
        val vaultFile = intent.getSerializableExtra(VIEW_PDF) as? VaultFile

        if (vaultFile != null) {
            this.vaultFile = vaultFile

            val vaultFileStream = MediaFileHandler.getStream(vaultFile)

            vaultFileStream?.let {
                displayFromUri(it)
            }
        }

        actionsDisabled = intent.hasExtra(PhotoViewerActivity.NO_ACTIONS)
    }

    private fun initObservers() {
        with(viewModel) {
            // Observer for the error LiveData, displays the error message when it is triggered
            error.observe(this@PDFReaderActivity) { errorResId ->
               onShowError(errorResId)
            }
            // Observer for the onMediaFileExportStatus LiveData, handles different export status cases
            onMediaFileExportStatus.observe(this@PDFReaderActivity) { status ->
                when (status) {
                    MediaFileExportStatus.EXPORT_START -> onExportStarted()
                    MediaFileExportStatus.EXPORT_PROGRESS -> onMediaExported()
                    MediaFileExportStatus.EXPORT_END -> onExportEnded()
                }
            }

            // Observer for the onMediaFileDeleted LiveData, handles the action when a media file is deleted
            onMediaFileDeleted.observe(this@PDFReaderActivity) { deleted ->
                if (deleted) onMediaFileDeleted()
            }

            // Observer for the onMediaFileRenamed LiveData, handles the action when a media file is renamed
            onMediaFileRenamed.observe(this@PDFReaderActivity) { renamed ->
                onMediaFileRename(renamed)
            }

            // Observer for the onMediaFileDeleteConfirmed LiveData, handles the action when media file deletion is confirmed
            onMediaFileDeleteConfirmed.observe(this@PDFReaderActivity) { mediaFileDeletedConfirmation ->
                onMediaFileDeleteConfirmation(
                    mediaFileDeletedConfirmation.vaultFile,
                    mediaFileDeletedConfirmation.showConfirmDelete
                )
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
    private fun onExportStarted() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun onExportEnded() {
        binding.progressBar.visibility = View.GONE
    }

    private fun onMediaFileDeleted() {
        MyApplication.bus().post(MediaFileDeletedEvent())
        finish()
    }

    private fun onMediaFileRename(vaultFile: VaultFile) {
        binding.toolbar.title = vaultFile.name
        MyApplication.bus().post(VaultFileRenameEvent())
    }

    private fun onMediaExported() {
        DialogUtils.showBottomMessage(
            this,
            resources.getQuantityString(R.plurals.gallery_toast_files_exported, 1, 1),
            false
        )
    }

    private fun displayFromUri(vaultFileStream: InputStream) {
        binding.pdfView.fromStream(vaultFileStream)
            .defaultPage(pageNumber)
            .onPageChange(this)
            .enableAnnotationRendering(true)
            .onLoad(this)
            .scrollHandle(DefaultScrollHandle(this))
            .spacing(10) // in dp
            .onPageError(this)
            .load()
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        pageNumber = page
        title = String.format("%s %s / %s", pdfFileName, page + 1, pageCount)
    }

    override fun loadComplete(nbPages: Int) {

    }

    @SuppressLint("LogNotTimber")
    override fun onPageError(page: Int, t: Throwable?) {
        Log.e("PDFViewActivity.TAG", "Cannot load page $page")
    }


    private fun setupToolbar() {

        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        binding.toolbar.title = vaultFile!!.name
        if (!actionsDisabled) {
            binding.toolbar.inflateMenu(R.menu.video_view_menu)
            vaultFile?.let { file ->
                setupMetadataMenuItem(file.metadata != null)
            }

            binding.toolbar.menu.findItem(R.id.menu_item_more)
                .setOnMenuItemClickListener {
                    vaultFile?.let { it1 ->
                        showVaultActionsDialog(
                            it1,
                            viewModel,
                            {
                                isInfoShown = true
                            },
                            toolbar = binding.toolbar
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
        val mdMenuItem = binding.toolbar.menu.findItem(R.id.menu_item_metadata)
        mdMenuItem.isVisible = visible
        if (visible) {
            mdMenuItem.setOnMenuItemClickListener {
                showMetadata()
                false
            }
        }
    }

    private fun showMetadata() {
        val viewMetadata = Intent(this, MetadataViewerActivity::class.java)
        viewMetadata.putExtra(Metadata.VIEW_METADATA, vaultFile)
        startActivity(viewMetadata)
    }
*/
}