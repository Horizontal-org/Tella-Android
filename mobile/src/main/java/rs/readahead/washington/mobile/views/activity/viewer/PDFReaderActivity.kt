package rs.readahead.washington.mobile.views.activity.viewer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.hzontal.tella_vault.Metadata.VIEW_METADATA
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.databinding.ActivityPdfReaderBinding
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.activity.MetadataViewerActivity
import rs.readahead.washington.mobile.views.activity.viewer.PermissionsActionsHelper.initContracts
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import java.io.InputStream


@AndroidEntryPoint
class PDFReaderActivity : BaseLockActivity() {
    private val viewModel: SharedMediaFileViewModel by viewModels()
    private lateinit var binding: ActivityPdfReaderBinding
    private var vaultFile: VaultFile? = null
    private var actionsDisabled = false
    private var isInfoShown = false
    private var pdfTopMargin = 0

    companion object {
        const val VIEW_PDF = "vp"
        const val HIDE_MENU = "hide_menu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initVaultMediaFile()
        initObservers()
        initContracts()
        setupToolbar()
    }

    private fun initVaultMediaFile() {
        val vaultFile = intent.getSerializableExtra(VIEW_PDF) as? VaultFile

        if (vaultFile != null) {
            this.vaultFile = vaultFile

            val vaultFileStream = MediaFileHandler.getStream(vaultFile)

            vaultFileStream?.let {
                displayFromUri(it)
            }
        }

        actionsDisabled = intent.hasExtra(HIDE_MENU)

        pdfTopMargin = resources.getDimensionPixelSize(R.dimen.pdf_top_margin)
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
        binding.pdfRendererView.initWithStream(vaultFileStream)
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

        binding.pdfRendererView.recyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            private val DIRECTION_NONE = -1
            private val DIRECTION_UP = 0
            private val DIRECTION_DOWN = 1
            var totalDy = 0


            var scrollDirection = DIRECTION_NONE
            var listStatus = RecyclerView.SCROLL_STATE_IDLE

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                listStatus = newState

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrollDirection = DIRECTION_NONE
                }

                if ( newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    if (getDragDirection() == DIRECTION_DOWN || isOnTop()) {
                        binding.toolbar.show()
                        val param =
                            binding.pdfRendererView.layoutParams as ViewGroup.MarginLayoutParams
                        param.setMargins(0, pdfTopMargin, 0, 0)
                        binding.pdfRendererView.layoutParams = param
                        binding.toolbar.outlineProvider = null

                    } else if (getDragDirection() == DIRECTION_UP) {
                        binding.toolbar.hide()
                        val param =
                            binding.pdfRendererView.layoutParams as ViewGroup.MarginLayoutParams
                        param.setMargins(0, 0, 0, 0)
                        binding.pdfRendererView.layoutParams = param
                        binding.pdfRendererView.outlineProvider = null
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                this.totalDy += dy
                scrollDirection = when {
                    dy > 0 -> DIRECTION_UP
                    dy < 0 -> DIRECTION_DOWN
                    else -> DIRECTION_NONE
                }
            }

            private fun isOnTop(): Boolean {
                return totalDy == 0
            }

            private fun getDragDirection(): Int {
                if (listStatus !=  RecyclerView.SCROLL_STATE_SETTLING) {
                    return DIRECTION_NONE
                }

                return when (scrollDirection) {
                    DIRECTION_NONE -> if (totalDy == 0) {
                        DIRECTION_DOWN  // drag down from top
                    } else {
                        DIRECTION_UP  // drag up from bottom
                    }

                    DIRECTION_UP -> DIRECTION_UP
                    DIRECTION_DOWN -> DIRECTION_DOWN
                    else -> DIRECTION_NONE
                }
            }
        })
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
        viewMetadata.putExtra(VIEW_METADATA, vaultFile)
        startActivity(viewMetadata)
    }

}