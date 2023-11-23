package rs.readahead.washington.mobile.views.activity.viewer

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import com.hzontal.tella_vault.VaultFile
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import dagger.hilt.android.AndroidEntryPoint
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.MediaFileDeletedEvent
import rs.readahead.washington.mobile.bus.event.VaultFileRenameEvent
import rs.readahead.washington.mobile.databinding.ActivityPdfReaderBinding
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.Security

@AndroidEntryPoint
class PDFReaderActivity : BaseLockActivity() {
    private val assetManager by lazy { assets }
    private var pageImage: Bitmap? = null
    private val viewModel: SharedMediaFileViewModel by viewModels()
    private lateinit var binding: ActivityPdfReaderBinding
    private var vaultFile: VaultFile? = null
    private var actionsDisabled = false

    companion object {
        const val VIEW_PDF = "vp"
        const val NO_ACTIONS = "na"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initVaultMediaFile()
        initObservers()
    }

    private fun initVaultMediaFile() {
        if (intent.hasExtra(VIEW_PDF)) {
            val vaultFile = intent.getSerializableExtra(VIEW_PDF) as VaultFile?
            if (vaultFile != null) {
                this.vaultFile = vaultFile
            }
        }
        if (intent.hasExtra(PhotoViewerActivity.NO_ACTIONS)) {
            actionsDisabled = true
        }
    }

    private fun initObservers() {
        with(viewModel) {
            // Observer for the error LiveData, displays the error message when it is triggered
            error.observe(this@PDFReaderActivity) {
                // onShowError(it)
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

    /**
     * Loads an existing PDF and renders it to a Bitmap
     */
    private fun renderFile(pdf: File) {
        // Render the page and save it to an image file
        try {
            // Load in an already created PDF
            val document: PDDocument = PDDocument.load(pdf)
            // Create a renderer for the document
            val renderer = PDFRenderer(document)
            // Render the image to an RGB Bitmap
            pageImage = renderer.renderImage(0, 1f, ImageType.RGB)


            val fileOut = FileOutputStream(pdf)
            pageImage?.compress(Bitmap.CompressFormat.JPEG, 100, fileOut)
            fileOut.close()
            //  tv.setText("Successfully rendered image to $path")
            // Optional: display the render result on screen
            displayRenderedImage()
        } catch (e: IOException) {
            //Log.e("PdfBox-Android-Sample", "Exception thrown while rendering file", e)
        }
    }

    /**
     * Helper method for drawing the result of renderFile() on screen
     */
    private fun displayRenderedImage() {
        object : Thread() {
            override fun run() {
                runOnUiThread {
                    val imageView =
                        findViewById<View>(R.id.renderedImageView) as ImageView
                    imageView.setImageBitmap(pageImage)
                }
            }
        }.start()
    }


    /**
     * Creates a simple pdf and encrypts it
     */
    fun createEncryptedPdf(pdf: File) {
        // val path: String = root.getAbsolutePath() + "/crypt.pdf"
        val keyLength = 128 // 128 bit is the highest currently supported

        // Limit permissions of those without the password
        val ap = AccessPermission()
        ap.setCanPrint(false)

        // Sets the owner password and user password
        val spp = StandardProtectionPolicy("12345", "hi", ap)

        // Setups up the encryption parameters
        spp.encryptionKeyLength = keyLength
        spp.permissions = ap
        val provider = BouncyCastleProvider()
        Security.addProvider(provider)
        val font: PDFont = PDType1Font.HELVETICA
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)
        try {
            val contentStream = PDPageContentStream(document, page)

            // Write Hello World in blue text
            contentStream.beginText()
            contentStream.setNonStrokingColor(15, 38, 192)
            contentStream.setFont(font, 12f)
            contentStream.newLineAtOffset(100f, 700f)
            contentStream.showText("Hello World")
            contentStream.endText()
            contentStream.close()

            // Save the final pdf document to a file
            document.protect(spp) // Apply the protections to the PDF
            document.save(pdf)
            document.close()
            //   tv.setText("Successfully wrote PDF to $path")
        } catch (e: IOException) {
            //  Log.e("PdfBox-Android-Sample", "Exception thrown while creating PDF for encryption", e)
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

}