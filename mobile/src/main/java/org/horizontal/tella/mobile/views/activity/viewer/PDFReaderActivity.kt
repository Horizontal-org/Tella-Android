package org.horizontal.tella.mobile.views.activity.viewer

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.hzontal.tella_vault.Metadata.VIEW_METADATA
import com.hzontal.tella_vault.VaultFile
import com.horizontal.pdfviewer.PdfRendererView
import com.horizontal.pdfviewer.annotations.PdfAnnotation
import com.horizontal.pdfviewer.annotations.PdfAnnotationOverlayView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.event.MediaFileDeletedEvent
import org.horizontal.tella.mobile.bus.event.VaultFileRenameEvent
import org.horizontal.tella.mobile.databinding.ActivityPdfReaderBinding
import org.horizontal.tella.mobile.media.MediaFileHandler
import org.horizontal.tella.mobile.views.activity.MetadataViewerActivity
import org.horizontal.tella.mobile.views.activity.viewer.PermissionsActionsHelper.initContracts
import org.horizontal.tella.mobile.views.activity.viewer.VaultActionsHelper.showVaultActionsDialog
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.horizontal.tella.mobile.util.show
import java.io.InputStream


/**
 * PDF reader activity with annotation support.
 *
 * 2025-08-19 (audit revision 6 — complete rewrite):
 *
 *  - Removed TTS feature entirely (was reading placeholder text).
 *  - Removed `onPause`/`onStop` overrides that caused re-lock-on-dialog.
 *  - Removed dead `onCreateOptionsMenu`/`onPrepareOptionsMenu`/`onOptionsItemSelected`
 *    (activity uses Toolbar directly, no `setSupportActionBar`).
 *  - Highlight + Sticky note: show color + size picker BEFORE entering mode.
 *  - Highlight: individual width (S/M/L) + height (S/M/L) options.
 *  - "Clear all" asks for confirmation.
 *  - Long-press shows "Copy text" context menu.
 *  - Mode indicator toast tells user how to exit mode.
 *  - Annotation updates are synchronous (no coroutine delay).
 *
 * 2025-08-20 (audit-fix rev 7):
 *
 *  - All AlertDialogs now go through [TellaDialogs.builder] so Save / OK /
 *    Cancel / Delete / Apply buttons are visible (white-on-white bug fix).
 *  - Annotations list dialog passes `onNavigate` so tapping a row first
 *    scrolls the PDF to that page, then opens the editor.
 *  - `copyPageText` distinguishes "no text" from "extractor unavailable"
 *    and shows a loading toast — see [copyPageText] docs.
 *  - `onConfigurationChanged` requests a re-layout of [PdfRendererView]
 *    so the PDF pages reflow correctly when the device rotates. The
 *    activity already declares `android:configChanges="...|orientation|..."`
 *    in the manifest so it isn't recreated on rotation — but the previous
 *    code didn't ask the RecyclerView to invalidate, so the page bitmaps
 *    kept their portrait dimensions and the user saw a half-empty page
 *    in landscape. We now explicitly `requestLayout` on the renderer view
 *    and notify the adapter of the dataset change so item views re-bind
 *    with the new width.
 */
@AndroidEntryPoint
class PDFReaderActivity : BaseLockActivity() {
    private val viewModel: SharedMediaFileViewModel by viewModels()
    private lateinit var binding: ActivityPdfReaderBinding
    private var vaultFile: VaultFile? = null
    private var actionsDisabled = false
    private var isInfoShown = false
    private var pdfTopMargin = 0

    // Highlight/sticky note style state (set by the style picker)
    private var highlightColor: Int = 0xFFFFFF00.toInt()  // yellow
    private var highlightWidthIndex: Int = 1  // medium
    private var highlightHeightIndex: Int = 1  // medium
    private var stickyNoteColor: Int = 0xFFE54A2D.toInt()  // red-orange
    private var stickySizeIndex: Int = 1  // medium

    companion object {
        const val VIEW_PDF = "vp"
        const val HIDE_MENU = "hide_menu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 2025-08-20 (audit-fix rev 9): set isManualOrientation = true BEFORE
        // super.onCreate() so BaseActivity.onCreate() does NOT force
        // requestedOrientation = SCREEN_ORIENTATION_PORTRAIT. The user
        // reported "the app always want to stay portrait when i open pdf
        // it show landscape for 1 sec after that go back to portrait".
        //
        // Root cause: BaseActivity.onCreate() line ~90 unconditionally calls
        // `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT`
        // unless isManualOrientation is true. PDFReaderActivity inherited
        // this, so even though the manifest declares configChanges=orientation
        // (no recreation on rotation), the activity was FORCED back to
        // portrait ~1 sec after opening. Setting isManualOrientation = true
        // here lets the PDF reader follow the device's actual orientation.
        isManualOrientation = true
        super.onCreate(savedInstanceState)
        binding = ActivityPdfReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdge(binding.root)
        initVaultMediaFile()
        initObservers()
        initContracts()
        setupToolbar()
    }

    /**
     * 2025-08-20 (audit-fix rev 7): the activity's manifest entry already
     * declares `android:configChanges="keyboard|keyboardHidden|orientation|
     * screenSize|screenLayout|smallestScreenSize|uiMode"` so the activity
     * is NOT recreated on rotation — but that means we are responsible for
     * reflowing the PDF on rotation. Without this override, the portrait
     * page bitmaps stayed in place after rotating to landscape, leaving
     * large empty bars on either side of the page.
     *
     * We now:
     *   1. Let the default `super.onConfigurationChanged` update resource
     *      configuration (so dimens / strings re-resolve).
     *   2. Ask the PdfRendererView to re-layout, which propagates down to
     *      the PinchZoomRecyclerView and individual page ImageView items.
     *   3. Notify the adapter that item dimensions may have changed —
     *      `notifyItemRangeChanged` with a payload-free signal triggers
     *      `onBindViewHolder` without re-rendering the page bitmap, which
     *      is fast and gives the ImageView the new width to fitCenter
     *      against.
     *
     * The PinchZoomRecyclerView's zoom factor is preserved across the
     * rotation because we don't touch its internal `mScaleFactor`.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-layout the renderer view so the RecyclerView picks up the new
        // width. Posting to the next frame gives the WindowManager time to
        // apply the new dimensions before we measure.
        binding.pdfRendererView.post {
            binding.pdfRendererView.requestLayout()
            // Force the adapter to re-bind visible items so each page's
            // ImageView picks up the new width via fitCenter. The page
            // bitmap itself doesn't need to be re-rendered.
            try {
                val lm = binding.pdfRendererView.recyclerView.layoutManager
                    as? androidx.recyclerview.widget.LinearLayoutManager
                val first = lm?.findFirstVisibleItemPosition() ?: 0
                val last = lm?.findLastVisibleItemPosition() ?: 0
                if (last >= first) {
                    binding.pdfRendererView.recyclerView.adapter
                        ?.notifyItemRangeChanged(first, last - first + 1)
                }
            } catch (_: Throwable) { /* best-effort */ }
        }
    }

    /**
     * 2025-08-20 (audit-fix rev 9): Cycles the activity's requested orientation
     * through three states: Auto-rotate (follow device) → Portrait → Landscape
     * → Auto-rotate. Shows a toast so the user knows which mode they're in.
     *
     * The activity already declares `android:configChanges` in the manifest
     * so it isn't recreated on orientation change — `onConfigurationChanged`
     * handles the reflow. `setRequestedOrientation` is asynchronous; the
     * actual rotation happens on the next configuration pass (~200ms).
     */
    private fun cycleOrientation() {
        val current = requestedOrientation
        val (next, label) = when (current) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ->
                Pair(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                     getString(R.string.pdf_rotate_landscape))
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ->
                Pair(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                     getString(R.string.pdf_rotate_auto))
            else ->
                Pair(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                     getString(R.string.pdf_rotate_portrait))
        }
        requestedOrientation = next
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
    }

    // ---- Annotation mode toggling ----

    private fun toggleMode(mode: PdfAnnotationOverlayView.AnnotationMode) {
        binding.pdfRendererView.annotationMode = mode
        refreshAnnotationMenuItemTitles()
    }

    private fun refreshAnnotationMenuItemTitles() {
        val highlightItem = binding.toolbar.menu.findItem(R.id.menu_item_pdf_highlight)
        val stickyItem = binding.toolbar.menu.findItem(R.id.menu_item_pdf_sticky)
        when (binding.pdfRendererView.annotationMode) {
            PdfAnnotationOverlayView.AnnotationMode.HIGHLIGHT -> {
                highlightItem?.title = getString(R.string.pdf_annot_title_off)
                stickyItem?.title = getString(R.string.pdf_annot_title_sticky_note)
            }
            PdfAnnotationOverlayView.AnnotationMode.STICKY_NOTE -> {
                highlightItem?.title = getString(R.string.pdf_annot_title_highlight)
                stickyItem?.title = getString(R.string.pdf_annot_title_off)
            }
            PdfAnnotationOverlayView.AnnotationMode.OFF -> {
                highlightItem?.title = getString(R.string.pdf_annot_title_highlight)
                stickyItem?.title = getString(R.string.pdf_annot_title_sticky_note)
            }
        }
    }

    private fun openAnnotationForEdit(annotation: PdfAnnotation) {
        when (annotation.type) {
            PdfAnnotation.Type.STICKY_NOTE -> PdfAnnotationDialogs.showStickyNoteEditor(
                context = this,
                annotation = annotation,
                onSave = { updated -> binding.pdfRendererView.updateAnnotation(updated) },
                onDelete = { ann -> binding.pdfRendererView.deleteAnnotation(ann.id) }
            )
            PdfAnnotation.Type.HIGHLIGHT -> PdfAnnotationDialogs.showHighlightEditor(
                context = this,
                annotation = annotation,
                onDelete = { ann -> binding.pdfRendererView.deleteAnnotation(ann.id) }
            )
        }
    }

    private fun initVaultMediaFile() {
        val vaultFile = intent.getSerializableExtra(VIEW_PDF) as? VaultFile
        if (vaultFile != null) {
            this.vaultFile = vaultFile
            val vaultFileStream = MediaFileHandler.getStream(vaultFile)
            vaultFileStream?.let {
                displayFromUri(it, vaultFile.id ?: vaultFile.hash ?: vaultFile.name)
            }
        }
        actionsDisabled = intent.hasExtra(HIDE_MENU)
        pdfTopMargin = resources.getDimensionPixelSize(R.dimen.pdf_top_margin)
    }

    private fun initObservers() {
        with(viewModel) {
            error.observe(this@PDFReaderActivity) { errorResId -> onShowError(errorResId) }
            onMediaFileExportStatus.observe(this@PDFReaderActivity) { status ->
                when (status) {
                    MediaFileExportStatus.EXPORT_START -> onExportStarted()
                    MediaFileExportStatus.EXPORT_PROGRESS -> onMediaExported()
                    MediaFileExportStatus.EXPORT_END -> onExportEnded()
                }
            }
            onMediaFileDeleted.observe(this@PDFReaderActivity) { deleted ->
                if (deleted) onMediaFileDeleted()
            }
            onMediaFileRenamed.observe(this@PDFReaderActivity) { renamed ->
                onMediaFileRename(renamed)
            }
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
                        if (isConfirmed) viewModel.deleteMediaFiles(vaultFile)
                    }
                }
            )
        } else {
            viewModel.deleteMediaFiles(vaultFile)
        }
    }

    private fun onShowError(errorResId: Int) {
        DialogUtils.showBottomMessage(this, getString(errorResId), true)
    }
    private fun onExportStarted() { binding.progressBar.visibility = View.VISIBLE }
    private fun onExportEnded() { binding.progressBar.visibility = View.GONE }
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

    private fun displayFromUri(vaultFileStream: InputStream, fileId: String) {
        binding.pdfRendererView.initWithStream(vaultFileStream, fileId)
        binding.pdfRendererView.annotationTapListener =
            PdfRendererView.AnnotationTapListener { ann -> openAnnotationForEdit(ann) }
        binding.pdfRendererView.longPressListener =
            com.horizontal.pdfviewer.annotations.PdfAnnotationOverlayView.LongPressListener { _, _, _ ->
                showLongPressMenu()
            }
        com.horizontal.pdfviewer.annotations.PdfTextExtractor.init(applicationContext)
        val resumed = binding.pdfRendererView.totalPageCount
        if (resumed > 0) {
            val saved = com.horizontal.pdfviewer.annotations.PdfReadingStateStore
                .get(this, fileId).getLastPage()
            if (saved > 0) {
                Toast.makeText(this, getString(R.string.pdf_annot_resumed_at, saved + 1, resumed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("unused")
    private fun displayFromUri(vaultFileStream: InputStream) {
        binding.pdfRendererView.initWithStream(vaultFileStream)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        binding.toolbar.title = vaultFile!!.name
        if (!actionsDisabled) {
            binding.toolbar.inflateMenu(R.menu.video_view_menu)
            binding.toolbar.inflateMenu(R.menu.pdf_annotation_menu)
            vaultFile?.let { file -> setupMetadataMenuItem(file.metadata != null) }

            binding.toolbar.menu.findItem(R.id.menu_item_more)
                .setOnMenuItemClickListener {
                    vaultFile?.let { it1 ->
                        showVaultActionsDialog(it1, viewModel, { isInfoShown = true }, toolbar = binding.toolbar)
                    }
                    false
                }

            binding.toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_pdf_highlight -> {
                        if (binding.pdfRendererView.annotationMode == PdfAnnotationOverlayView.AnnotationMode.HIGHLIGHT) {
                            // Already in highlight mode — turn off
                            toggleMode(PdfAnnotationOverlayView.AnnotationMode.OFF)
                        } else {
                            // Show style picker first
                            PdfAnnotationStylePicker.showHighlightPicker(
                                context = this,
                                currentColor = highlightColor,
                                currentWidthIndex = highlightWidthIndex,
                                currentHeightIndex = highlightHeightIndex
                            ) { result ->
                                highlightColor = result.color
                                highlightWidthIndex = result.widthIndex
                                highlightHeightIndex = result.heightIndex
                                binding.pdfRendererView.setHighlightColor(result.color)
                                binding.pdfRendererView.setHighlightSize(
                                    PdfAnnotationStylePicker.widthMultiplier(result.widthIndex),
                                    PdfAnnotationStylePicker.heightMultiplier(result.heightIndex)
                                )
                                toggleMode(PdfAnnotationOverlayView.AnnotationMode.HIGHLIGHT)
                                Toast.makeText(this, R.string.pdf_annot_hint_tap_to_highlight, Toast.LENGTH_LONG).show()
                            }
                        }
                        true
                    }
                    R.id.menu_item_pdf_sticky -> {
                        if (binding.pdfRendererView.annotationMode == PdfAnnotationOverlayView.AnnotationMode.STICKY_NOTE) {
                            toggleMode(PdfAnnotationOverlayView.AnnotationMode.OFF)
                        } else {
                            PdfAnnotationStylePicker.showStickyNotePicker(
                                context = this,
                                currentColor = stickyNoteColor,
                                currentSizeIndex = stickySizeIndex
                            ) { result ->
                                stickyNoteColor = result.color
                                stickySizeIndex = result.sizeIndex
                                binding.pdfRendererView.setStickyNoteColor(result.color)
                                binding.pdfRendererView.setStickyNoteSize(
                                    PdfAnnotationStylePicker.sizeMultiplier(result.sizeIndex)
                                )
                                toggleMode(PdfAnnotationOverlayView.AnnotationMode.STICKY_NOTE)
                                Toast.makeText(this, R.string.pdf_annot_hint_tap_for_note, Toast.LENGTH_LONG).show()
                            }
                        }
                        true
                    }
                    R.id.menu_item_pdf_goto_page -> {
                        com.horizontal.pdfviewer.annotations.PdfGoToPageDialog.show(
                            context = this,
                            pageCount = binding.pdfRendererView.totalPageCount,
                            onGo = { page -> binding.pdfRendererView.scrollToPage(page) }
                        )
                        true
                    }
                    R.id.menu_item_pdf_list -> {
                        // 2025-08-20 (audit-fix rev 7): pass onNavigate so
                        // tapping a list row first scrolls the PDF to the
                        // page the annotation lives on, then opens the
                        // editor. The list dialog already shows the page
                        // number on each row (e.g. "Page 12 — text…").
                        PdfAnnotationDialogs.showAnnotationList(
                            context = this,
                            annotations = binding.pdfRendererView.listAnnotations(),
                            onSelected = { ann -> openAnnotationForEdit(ann) },
                            onNavigate = { ann ->
                                binding.pdfRendererView.scrollToPage(ann.page)
                            }
                        )
                        true
                    }
                    R.id.menu_item_pdf_copy_text -> {
                        copyPageText()
                        true
                    }
                    R.id.menu_item_pdf_rotate -> {
                        // 2025-08-20 (audit-fix rev 9): manual rotate button.
                        // Cycles requestedOrientation through
                        // UNSPECIFIED (auto) → PORTRAIT → LANDSCAPE → UNSPECIFIED.
                        cycleOrientation()
                        true
                    }
                    R.id.menu_item_pdf_share -> {
                        // 2025-08-20 (audit-fix rev 8): dedicated Share menu
                        // item — asks the user whether to bake annotations
                        // into the PDF or share the original.
                        showSharePdfDialog()
                        true
                    }
                    R.id.menu_item_pdf_clear -> {
                        confirmClearAllAnnotations()
                        true
                    }
                    else -> false
                }
            }
        }
        // 2026-08-20 (audit-fix rev 10): store the scroll listener reference
        // so the zoom listener can update its isZoomed flag.
        val scrollListener = PdfScrollListener(binding.toolbar, binding.pdfRendererView, pdfTopMargin)
        binding.pdfRendererView.recyclerView.addOnScrollListener(scrollListener)
        // 2026-08-20 (audit-fix rev 10): when the user zooms in, the normal
        // scroll-based toolbar show/hide stops working because the canvas
        // transform (translate + scale) is applied at draw time, not at
        // scroll time — the RecyclerView's scroll position doesn't change
        // when the user pans a zoomed page, so PdfScrollListener never
        // fires. To fix "after I zoom I can't see the nav bar anymore when
        // I scroll down", we listen for zoom changes and FORCE the toolbar
        // visible whenever the user is zoomed in (> 1.05×). When they zoom
        // back to 1×, normal scroll-based show/hide resumes.
        (binding.pdfRendererView.recyclerView as? com.horizontal.pdfviewer.PinchZoomRecyclerView)
            ?.setOnZoomChangeListener { scaleFactor ->
                val zoomed = scaleFactor > 1.05f
                scrollListener.isZoomed = zoomed
                if (zoomed) {
                    // Zoomed in — force toolbar visible + restore top margin
                    // so the toolbar doesn't overlap the page content.
                    binding.toolbar.show()
                    val param = binding.pdfRendererView.layoutParams as android.view.ViewGroup.MarginLayoutParams
                    param.setMargins(0, pdfTopMargin, 0, 0)
                    binding.pdfRendererView.layoutParams = param
                }
                // When scaleFactor <= 1.05 (back to 1×), do nothing — the
                // PdfScrollListener will handle show/hide on the next scroll.
            }
    }

    private fun confirmClearAllAnnotations() {
        // 2025-08-20 (audit-fix rev 7): TellaDialogs.builder applies the
        // TellaDialogTheme overlay so Delete / Cancel are visible.
        TellaDialogs.builder(this)
            .setTitle(R.string.pdf_annot_dialog_delete_title)
            .setMessage(R.string.pdf_annot_clear_confirm_message)
            .setPositiveButton(R.string.pdf_annot_dialog_delete) { d, _ ->
                binding.pdfRendererView.clearAllAnnotations()
                d.dismiss()
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun showLongPressMenu() {
        val items = arrayOf(
            getString(R.string.pdf_annot_long_press_copy),
            getString(R.string.pdf_annot_long_press_copy_image)
        )
        TellaDialogs.builder(this)
            .setItems(items) { d, idx ->
                when (idx) {
                    0 -> copyPageText()
                    1 -> Toast.makeText(this, "Copy image: coming soon", Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    /**
     * 2025-08-20 (audit-fix rev 7): the previous implementation always
     * showed "this page has no selectable text (it may be a scanned
     * image)" — even for normal text PDFs — because [PdfTextExtractor]
     * was broken (it rendered the page to a Bitmap, immediately
     * `recycle()`d it, and returned an empty StringBuilder).
     *
     * The extractor is now backed by `com.tom-roush:pdfbox-android`
     * which actually parses the PDF text stream. We distinguish three
     * outcomes:
     *
     *   - `text.length > 0`  → copy to clipboard, toast "copied"
     *   - `text.isEmpty()` AND extractor loaded OK → toast "no text"
     *     (this is now legitimately a scanned-image / text-less PDF)
     *   - extractor not loaded (lib failed to init) → toast "unavailable"
     *
     * The misleading "(it may be a scanned image)" suffix has been
     * dropped from the empty case because the user reported it as false
     * information — it appeared for every PDF, not just scanned ones.
     */
    private fun copyPageText() {
        val fileId = vaultFile?.id ?: vaultFile?.hash ?: vaultFile?.name ?: return
        val pageNo = binding.pdfRendererView.currentPageIndex
        val pageCount = binding.pdfRendererView.totalPageCount
        // 2025-08-20 (audit-fix rev 7): show a non-blocking progress toast
        // so the user sees the extraction is happening, not that the app
        // froze. PDFBox can take 300–800 ms on a 1-page complex PDF.
        val loadingToast = Toast.makeText(this, R.string.pdf_annot_copy_text_loading, Toast.LENGTH_SHORT)
        loadingToast.show()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val vaultFile = this@PDFReaderActivity.vaultFile ?: return@launch
                val stream = MediaFileHandler.getStream(vaultFile)
                if (stream != null) {
                    com.horizontal.pdfviewer.annotations.PdfTextExtractor.load(fileId, stream)
                }
                val text = com.horizontal.pdfviewer.annotations.PdfTextExtractor.extractText(fileId, pageNo, pageCount)
                val extractorAvailable = com.horizontal.pdfviewer.annotations.PdfTextExtractor.isAvailable(fileId)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    loadingToast.cancel()
                    if (text.isNotBlank()) {
                        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("PDF page text", text))
                        Toast.makeText(this@PDFReaderActivity, R.string.pdf_annot_copy_text_done, Toast.LENGTH_SHORT).show()
                    } else if (extractorAvailable) {
                        // Genuine "no text" case — the extractor loaded the
                        // PDF successfully and parsed the page, but the
                        // page has zero characters. That's a scanned image
                        // or a text-less vector PDF.
                        Toast.makeText(this@PDFReaderActivity, R.string.pdf_annot_copy_text_empty, Toast.LENGTH_LONG).show()
                    } else {
                        // Extractor not initialised — library missing or
                        // init failed. Don't lie about "scanned image".
                        Toast.makeText(this@PDFReaderActivity, R.string.pdf_annot_copy_text_unavailable, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    loadingToast.cancel()
                    // 2025-08-20 (audit-fix rev 7): surface the real
                    // failure mode rather than misleading "no text".
                    Toast.makeText(this@PDFReaderActivity, R.string.pdf_annot_copy_text_unavailable, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupMetadataMenuItem(visible: Boolean) {
        if (actionsDisabled) return
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

    /**
     * 2025-08-20 (audit-fix rev 8): Shows the "Share PDF" dialog asking the
     * user whether to bake annotations into the PDF or share the original.
     *
     * If the PDF has no annotations, we skip the dialog and share the
     * original directly (with a toast explaining why).
     *
     * If the user picks "Share with annotations", we run
     * [PdfAnnotationFlattener.flatten] on an IO coroutine (it loads the
     * full PDF into memory via PDFBox, which takes 200-800ms for a typical
     * PDF), show a progress dialog, then share the flattened file via the
     * existing [MediaFileHandler.startShareActivity] path.
     *
     * If flattening fails (e.g. the PDF is encrypted in a way PDFBox can't
     * read), we fall back to sharing the original with a toast.
     */
    private fun showSharePdfDialog() {
        val annotations = binding.pdfRendererView.listAnnotations()
        if (annotations.isEmpty()) {
            // No annotations — just share the original.
            Toast.makeText(this, R.string.pdf_share_no_annotations, Toast.LENGTH_SHORT).show()
            shareOriginalPdf()
            return
        }

        // Show a chooser: "Share with annotations" vs "Share original".
        val items = arrayOf(
            getString(R.string.pdf_share_with_annotations),
            getString(R.string.pdf_share_original)
        )
        TellaDialogs.builder(this)
            .setTitle(R.string.pdf_share_title)
            .setItems(items) { d, idx ->
                d.dismiss()
                when (idx) {
                    0 -> sharePdfWithAnnotations(annotations)
                    1 -> shareOriginalPdf()
                }
            }
            .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
            .show()
    }

    /**
     * Shares the original PDF (no annotations) via the standard
     * [MediaFileHandler.startShareActivity] path. This is the same path
     * the existing "more" overflow → share button uses.
     */
    private fun shareOriginalPdf() {
        val vf = vaultFile ?: return
        // The existing share path handles the encrypted-uri plumbing.
        org.horizontal.tella.mobile.media.MediaFileHandler.startShareActivity(this, vf, false)
    }

    /**
     * Bakes the annotations into a new PDF file (via PDFBox) and shares it.
     *
     * Runs on an IO coroutine because PDFBox loads the entire PDF into
     * memory and renders the annotations, which takes 200-800ms for a
     * typical PDF. A progress dialog is shown on the main thread while
     * this runs.
     *
     * If flattening fails, we fall back to sharing the original with a
     * toast explaining the failure.
     */
    private fun sharePdfWithAnnotations(annotations: List<com.horizontal.pdfviewer.annotations.PdfAnnotation>) {
        val vf = vaultFile ?: return
        val fileId = vf.id ?: vf.hash ?: vf.name ?: return
        val progressToast = Toast.makeText(this, R.string.pdf_share_flattening_progress, Toast.LENGTH_LONG)
        progressToast.show()

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val stream = org.horizontal.tella.mobile.media.MediaFileHandler.getStream(vf)
                if (stream == null) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        progressToast.cancel()
                        Toast.makeText(this@PDFReaderActivity, R.string.pdf_share_flattening_failed, Toast.LENGTH_LONG).show()
                        shareOriginalPdf()
                    }
                    return@launch
                }
                val outputName = "${vf.name ?: "document"}_annotated.pdf"
                val flattenedFile = com.horizontal.pdfviewer.annotations.PdfAnnotationFlattener.flatten(
                    context = applicationContext,
                    inputStream = stream,
                    annotations = annotations,
                    outputFileName = outputName
                )
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    progressToast.cancel()
                    if (flattenedFile != null && flattenedFile.exists() && flattenedFile.length() > 0) {
                        // 2025-08-20 (audit-fix rev 9): use the NEW PlainFileProvider
                        // authority instead of EncryptedFileProvider. The encrypted
                        // provider intercepts openFile() and tries to read from the
                        // encrypted vault — which fails for cache files (the flattened
                        // PDF is NOT in the vault) and produces a 0-byte response.
                        // PlainFileProvider serves the file directly from disk.
                        val authority = "${packageName}.PlainFileProvider"
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            this@PDFReaderActivity,
                            authority,
                            flattenedFile,
                            flattenedFile.name
                        )
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            type = "application/pdf"
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        }
                        val chooser = android.content.Intent.createChooser(shareIntent, getString(R.string.pdf_share_title))
                        // 2025-08-20 (audit-fix rev 9): FLAG_GRANT_READ_URI_PERMISSION
                        // (not FLAG_GRANT_PERSISTABLE_URI_PERMISSION — that flag only
                        // marks the URI as persistable, it does NOT grant read access).
                        chooser.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try {
                            startActivity(chooser)
                        } catch (e: Exception) {
                            Toast.makeText(this@PDFReaderActivity, R.string.pdf_share_flattening_failed, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@PDFReaderActivity, R.string.pdf_share_flattening_failed, Toast.LENGTH_LONG).show()
                        shareOriginalPdf()
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    progressToast.cancel()
                    Toast.makeText(this@PDFReaderActivity, R.string.pdf_share_flattening_failed, Toast.LENGTH_LONG).show()
                    shareOriginalPdf()
                }
            }
        }
    }

    override fun onDestroy() {
        try { binding.pdfRendererView.saveCurrentReadingState() } catch (_: Throwable) {}
        try { binding.pdfRendererView.closePdfRender() } catch (_: Throwable) {}
        vaultFile?.id?.let { fid ->
            try { com.horizontal.pdfviewer.annotations.PdfTextExtractor.close(fid) } catch (_: Throwable) {}
        }
        super.onDestroy()
    }
}
