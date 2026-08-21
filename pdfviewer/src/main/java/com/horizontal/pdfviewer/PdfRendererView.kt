package com.horizontal.pdfviewer

import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.horizontal.pdfviewer.annotations.PdfAnnotation
import com.horizontal.pdfviewer.annotations.PdfAnnotationOverlayView
import com.horizontal.pdfviewer.annotations.PdfAnnotationStore
import com.horizontal.pdfviewer.annotations.PdfReadingStateStore
import com.horizontal.pdfviewer.util.ParcelFileDescriptorUtil
import com.horizontal.pdfviewer.util.PdfEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * Created by Rajat on 11,July,2020
 *
 * 2025-08-19 (audit): The view now accepts a stable [fileId] in every
 * `initWith*` call. That id keys two persistent stores:
 *
 *   1. [PdfReadingStateStore] — last visible page index and scroll offset.
 *      On init the view restores that position so the user reopens the PDF
 *      on the exact page they left off.
 *
 *   2. [PdfAnnotationStore] — every sticky note and highlight created by the
 *      user via the new [PdfAnnotationOverlayView] is persisted by id and
 *      re-attached on the next open.
 *
 * Public API surface is backwards compatible: callers that still call
 * `initWithStream(stream)` get the legacy in-memory behaviour with no
 * persistence. Callers that pass an explicit fileId get the new behaviour.
 */
class PdfRendererView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), LifecycleObserver {
    lateinit var recyclerView: RecyclerView
    private lateinit var pageNo: TextView
    private lateinit var pdfRendererCore: PdfRendererCore
    private lateinit var pdfViewAdapter: PdfViewAdapter
    private var engine = PdfEngine.INTERNAL
    private var showDivider = true
    private var divider: Drawable? = null
    private var runnable = Runnable {}
    private var enableLoadingForPages: Boolean = false
    private var pdfRendererCoreInitialised = false
    private var pageMargin: Rect = Rect(0, 0, 0, 0)
    var statusListener: StatusCallBack? = null
    private var positionToUseForState: Int = 0
    private var restoredScrollPosition: Int = NO_POSITION
    private var disableScreenshots: Boolean = false

    // ----- 2025-08-19 (audit): new state for persistence + annotations -----

    /**
     * Stable id used as the key for [PdfReadingStateStore] and [PdfAnnotationStore].
     * Set on every `initWith*(stream, fileId)` call. Null = legacy behaviour.
     */
    private var fileId: String? = null

    /** True when the host has explicitly enabled annotation features. */
    private var annotationsEnabled: Boolean = false

    /** Current overlay mode — forwarded into the adapter on every change. */
    var annotationMode: PdfAnnotationOverlayView.AnnotationMode =
        PdfAnnotationOverlayView.AnnotationMode.OFF
        set(value) {
            field = value
            if (this::pdfViewAdapter.isInitialized) {
                pdfViewAdapter.annotationMode = value
            }
        }

    /** Annotation tap (open / edit / delete) callback surfaced to the host. */
    var annotationTapListener: AnnotationTapListener? = null
        set(value) {
            field = value
            applyAnnotationTapListener(value)
        }

    // 2025-08-19 (audit rev6): color + size setters forwarded to the overlay.
    fun setHighlightColor(color: Int) {
        if (this::pdfViewAdapter.isInitialized) pdfViewAdapter.highlightColor = color
    }
    fun setHighlightSize(widthMultiplier: Float, heightMultiplier: Float) {
        if (this::pdfViewAdapter.isInitialized) {
            pdfViewAdapter.highlightWidthMultiplier = widthMultiplier
            pdfViewAdapter.highlightHeightMultiplier = heightMultiplier
        }
    }
    fun setStickyNoteColor(color: Int) {
        if (this::pdfViewAdapter.isInitialized) pdfViewAdapter.stickyNoteColor = color
    }
    fun setStickyNoteSize(multiplier: Float) {
        if (this::pdfViewAdapter.isInitialized) pdfViewAdapter.stickyNoteSizeMultiplier = multiplier
    }
    var longPressListener: PdfAnnotationOverlayView.LongPressListener? = null
        set(value) {
            field = value
            if (this::pdfViewAdapter.isInitialized) pdfViewAdapter.longPressListener = value
        }

    /** Public functional interface (host-side). Lets the host react to taps
     *  on existing annotations (open editor / confirm delete / etc.). */
    fun interface AnnotationTapListener {
        fun onAnnotationTapped(annotation: PdfAnnotation)
    }

    /** Bridges the host's public tap listener to the adapter's internal type. */
    private fun applyAnnotationTapListener(value: AnnotationTapListener?) {
        if (!this::pdfViewAdapter.isInitialized) return
        pdfViewAdapter.annotationTapListener = value?.let { hostListener ->
            PdfViewAdapter.AnnotationTapListener { ann -> hostListener.onAnnotationTapped(ann) }
        }
    }

    /**
     * Lazy-built bridge between overlay drag gestures and [PdfAnnotationStore].
     * Lives for the lifetime of the view, so we keep a single instance.
     */
    private val annotationListener = object : PdfAnnotationOverlayView.AnnotationListener {
        override fun onAnnotationCreated(annotation: PdfAnnotation) {
            val fid = fileId ?: return
            PdfAnnotationStore.get(context, fid).add(annotation)
            reloadAnnotationsFromStore()
        }

        override fun onStickyNoteRequested(annotation: PdfAnnotation) {
            // Create the sticky note placeholder first so it shows on screen,
            // then ask the host to open the editor. The host updates the text
            // via [updateAnnotation].
            val fid = fileId ?: return
            val saved = PdfAnnotationStore.get(context, fid).add(annotation)
            reloadAnnotationsFromStore()
            annotationTapListener?.onAnnotationTapped(saved)
        }
    }

    val totalPageCount: Int
        get() {
            return pdfRendererCore.getPageCount()
        }

    init {
        getAttrs(attrs, defStyleAttr)
    }

    interface StatusCallBack {
        fun onPdfLoadStart() {}
        fun onPdfLoadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long?) {}
        fun onPdfLoadSuccess(absolutePath: String) {}
        fun onError(error: Throwable) {}
        fun onPageChanged(currentPage: Int, totalPage: Int) {}
    }

    fun initWithFile(file: File) {
        init(file)
    }

    fun initWithFile(file: File, fileId: String) {
        this.fileId = fileId
        annotationsEnabled = true
        init(file)
        restoreFromPersistentState()
    }

    @Throws(FileNotFoundException::class)
    fun initWithUri(uri: Uri) {
        val fileDescriptor =
            context.contentResolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException()
        init(fileDescriptor)
    }

    @Throws(FileNotFoundException::class)
    fun initWithUri(uri: Uri, fileId: String) {
        this.fileId = fileId
        annotationsEnabled = true
        val fileDescriptor =
            context.contentResolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException()
        init(fileDescriptor)
        restoreFromPersistentState()
    }

    @Throws(IOException::class)
    fun initWithStream(inputStream: InputStream) {
        val fileDescriptor = ParcelFileDescriptorUtil.pipeFrom(inputStream)
        init(fileDescriptor)
    }

    /**
     * 2025-08-19 (audit): preferred entry point — pass a stable [fileId]
     * so reading state and annotations are persisted between launches.
     */
    @Throws(IOException::class)
    fun initWithStream(inputStream: InputStream, fileId: String) {
        this.fileId = fileId
        annotationsEnabled = true
        val fileDescriptor = ParcelFileDescriptorUtil.pipeFrom(inputStream)
        init(fileDescriptor)
        restoreFromPersistentState()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val savedState = Bundle()
        savedState.putParcelable("superState", superState)
        if (this::recyclerView.isInitialized) {
            savedState.putInt("scrollPosition", positionToUseForState)
        }
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val superState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                state.getParcelable("superState", Parcelable::class.java)
            } else {
                state.getParcelable("superState")
            }
            super.onRestoreInstanceState(superState)
            restoredScrollPosition = state.getInt("scrollPosition", positionToUseForState)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private fun init(file: File) {
        val fileDescriptor = PdfRendererCore.getFileDescriptor(file)
        init(fileDescriptor)
    }

    private fun init(fileDescriptor: ParcelFileDescriptor) {
        // 2025-08-19 (audit-fix): tear down any previous renderer first.
        // The original code never called closePdfRender() before
        // re-initialising — so opening a second PDF leaked the previous
        // ParcelFileDescriptor + PdfRenderer + bitmap cache, and the
        // app would crash with "PdfRenderer has been closed" or OOM.
        // Defensive close keeps the singleton-safe pattern below intact.
        try {
            if (pdfRendererCoreInitialised) {
                pdfRendererCore.closePdfRender()
                pdfRendererCoreInitialised = false
            }
        } catch (_: Throwable) { /* best-effort */ }

        // Proceed with safeFile
        pdfRendererCore = PdfRendererCore(context, fileDescriptor)
        pdfRendererCoreInitialised = true
        pdfViewAdapter = PdfViewAdapter(context, pdfRendererCore, pageMargin, enableLoadingForPages)
        val v = LayoutInflater.from(context).inflate(R.layout.pdf_rendererview, this, false)
        // 2025-08-19 (audit-fix): avoid adding the layout twice when
        // init() is called multiple times on the same view (e.g. the
        // activity is recreated without a fresh PdfRendererView).
        // removeAllViews() also detaches the existing RecyclerView so
        // its scroll listeners don't fire on a stale adapter.
        if (childCount > 0) removeAllViews()
        addView(v)
        recyclerView = findViewById(R.id.recyclerView)
        pageNo = findViewById(R.id.pageNumber)
        recyclerView.apply {
            adapter = pdfViewAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            if (showDivider) {
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                    divider?.let { setDrawable(it) }
                }.let { addItemDecoration(it) }
            }
            addOnScrollListener(scrollListener)
        }

        // Wire annotation pipeline
        pdfViewAdapter.annotationListener = annotationListener
        pdfViewAdapter.annotationMode = annotationMode
        pdfViewAdapter.longPressListener = longPressListener
        applyAnnotationTapListener(annotationTapListener)
        if (annotationsEnabled) reloadAnnotationsFromStore()

        // 2025-08-20 (audit-fix rev 9): ONLY scroll to the restored position
        // if it's a REAL saved position (> 0). The previous version scrolled
        // to `restoredScrollPosition` even when it was 0 (the default for a
        // first-time-open PDF), which caused the PDF to jump to page 1
        // ~300ms after the activity opened — exactly when the user was
        // tapping the toolbar to open the highlight/sticky picker for the
        // first time. The user reported "when I first time start it take
        // me to page 1 thats a bad experience".
        //
        // Now we skip the scroll entirely if:
        //   - restoredScrollPosition is NO_POSITION (no saved state), OR
        //   - restoredScrollPosition is 0 (first page — no need to scroll,
        //     the RecyclerView already starts at page 0).
        Handler(Looper.getMainLooper()).postDelayed({
            if (restoredScrollPosition != NO_POSITION && restoredScrollPosition > 0) {
                recyclerView.scrollToPosition(restoredScrollPosition)
                restoredScrollPosition = NO_POSITION  // Reset after applying
            } else {
                restoredScrollPosition = NO_POSITION  // Clear without scrolling
            }
        }, 300)

        runnable = Runnable {
            pageNo.visibility = View.GONE
        }
    }

    /**
     * Called after [init] when the host has provided a [fileId]. Restores the
     * last-read page and triggers annotation re-binding.
     *
     * 2025-08-20 (audit-fix rev 9): only scroll if the saved page is > 0.
     * Scrolling to page 0 is a no-op (the RecyclerView starts there) but
     * the programmatic `scrollToPositionWithOffset(0, 0)` call was triggering
     * `PdfScrollListener.onScrollStateChanged(SCROLL_STATE_SETTLING)` which
     * caused visual jitter. Now we skip the call entirely for page 0.
     */
    private fun restoreFromPersistentState() {
        val fid = fileId ?: return
        val savedPage = PdfReadingStateStore.get(context, fid).getLastPage()
        val offsetDp = PdfReadingStateStore.get(context, fid).getLastScrollOffsetDp()
        val offsetPx = (offsetDp * resources.displayMetrics.density).toInt()
        restoredScrollPosition = savedPage.coerceAtLeast(0)
        // 2025-08-20 (audit-fix rev 9): only post the scroll if there's a
        // real saved page (> 0). Page 0 is the default start position, so
        // scrolling to it is a no-op that just causes visual jitter.
        if (savedPage <= 0) return
        Handler(Looper.getMainLooper()).postDelayed({
            if (this::recyclerView.isInitialized) {
                val lm = recyclerView.layoutManager as? LinearLayoutManager
                // Scroll the page to the top so the user lands at the start of
                // the page they left off on; if the page was already on screen
                // we additionally offset by the remembered pixel offset.
                lm?.scrollToPositionWithOffset(savedPage, -offsetPx)
            }
        }, 500)
    }

    /**
     * Re-reads annotations from [PdfAnnotationStore] and pushes them into the
     * adapter so every page overlay is re-bound. Cheap — single SharedPreferences
     * + JSON parse — so safe to call after every mutation.
     */
    fun reloadAnnotationsFromStore() {
        val fid = fileId ?: return
        if (!this::pdfViewAdapter.isInitialized) return
        val items = PdfAnnotationStore.get(context, fid).annotations
        // 2025-08-19 (audit rev6): call synchronously — no coroutine.
        // The previous version used CoroutineScope(Dispatchers.Main).launch {}
        // which caused a 1-frame delay — the user saw the annotation appear
        // "late" after creating it. Calling directly is safe because
        // setAnnotations uses a payload that only updates the overlay, not
        // the page bitmap.
        pdfViewAdapter.setAnnotations(items)
    }

    /** Persists the current page index for the open document. */
    fun saveCurrentReadingState() {
        val fid = fileId ?: return
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = lm.findFirstVisibleItemPosition().coerceAtLeast(0)
        val offset = lm.findViewByPosition(firstVisible)?.top ?: 0
        val density = resources.displayMetrics.density
        PdfReadingStateStore.get(context, fid).saveLastPage(
            pageIndex = firstVisible,
            scrollOffsetPx = -offset,
            density = if (density > 0f) density else 1f
        )
    }

    /**
     * 2025-08-19 (audit-fix): debounced wrapper around [saveCurrentReadingState].
     * Coalesces a burst of position-change callbacks (e.g. during a fast
     * fling) into a single disk write 250ms after the last change. The
     * immediate write is still performed on scroll-settle (see
     * [scrollListener.onScrollStateChanged]) and on activity pause/stop
     * (see [PDFReaderActivity.onPause] / [onStop]).
     */
    private val saveStateHandler = Handler(Looper.getMainLooper())
    private val saveStateRunnable = Runnable { saveCurrentReadingState() }

    private fun scheduleThrottledSave() {
        saveStateHandler.removeCallbacks(saveStateRunnable)
        saveStateHandler.postDelayed(saveStateRunnable, SAVE_DEBOUNCE_MS)
    }

    private fun cancelThrottledSave() {
        saveStateHandler.removeCallbacks(saveStateRunnable)
    }

    companion object {
        /** Debounce window for in-flight scroll-position saves. 250ms is
         *  short enough that the user never notices a stale "last page"
         *  if they background the app mid-scroll, but long enough that
         *  a fast fling over 50 pages only writes to disk once. */
        private const val SAVE_DEBOUNCE_MS: Long = 250L
    }

    /**
     * 2025-08-19 (audit / Feature 1.B): jump to a specific page index.
     * Used by the Go-to-Page dialog. Page index is zero-based and clamped
     * to the document's actual page count.
     */
    fun scrollToPage(pageIndex: Int) {
        if (!this::recyclerView.isInitialized) return
        val target = pageIndex.coerceIn(0, (totalPageCount - 1).coerceAtLeast(0))
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
        // scrollToPositionWithOffset places the page at the top of the
        // viewport (offset 0) so the user lands cleanly at the start of
        // the page they requested.
        lm.scrollToPositionWithOffset(target, 0)
        // Update the persisted state so closing right after the jump keeps
        // the new position rather than the previous scroll position.
        positionToUseForState = target
        saveCurrentReadingState()
    }

    /**
     * 2025-08-19 (audit / Feature 1.C): zero-based page index of the page
     * currently at the top of the viewport. Used by the TTS controller so
     * it knows which page to read first.
     */
    val currentPageIndex: Int
        get() {
            if (!this::recyclerView.isInitialized) return 0
            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return 0
            val pos = lm.findFirstVisibleItemPosition()
            return if (pos == RecyclerView.NO_POSITION) 0 else pos
        }

    /** Append-only helper exposed for the host activity (debug / audit / export). */
    fun listAnnotations(): List<PdfAnnotation> {
        val fid = fileId ?: return emptyList()
        return PdfAnnotationStore.get(context, fid).annotations
    }

    /** Update an existing annotation (e.g. user edited sticky note text). */
    fun updateAnnotation(annotation: PdfAnnotation) {
        val fid = fileId ?: return
        PdfAnnotationStore.get(context, fid).update(annotation)
        reloadAnnotationsFromStore()
    }

    /** Remove an annotation by id. */
    fun deleteAnnotation(id: String) {
        val fid = fileId ?: return
        PdfAnnotationStore.get(context, fid).delete(id)
        reloadAnnotationsFromStore()
    }

    /** Delete every annotation for the open document. */
    fun clearAllAnnotations() {
        val fid = fileId ?: return
        PdfAnnotationStore.get(context, fid).clearAll()
        reloadAnnotationsFromStore()
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        private var lastFirstVisiblePosition = NO_POSITION
        private var lastCompletelyVisiblePosition = NO_POSITION

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager

            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val firstCompletelyVisiblePosition =
                layoutManager.findFirstCompletelyVisibleItemPosition()
            val isPositionChanged = firstVisiblePosition != lastFirstVisiblePosition ||
                    firstCompletelyVisiblePosition != lastCompletelyVisiblePosition
            if (isPositionChanged) {
                val positionToUse = if (firstCompletelyVisiblePosition != NO_POSITION) {
                    firstCompletelyVisiblePosition
                } else {
                    firstVisiblePosition
                }
                positionToUseForState = positionToUse
                updatePageNumberDisplay(positionToUse)
                lastFirstVisiblePosition = firstVisiblePosition
                lastCompletelyVisiblePosition = firstCompletelyVisiblePosition
                // 2025-08-19 (audit-fix): the previous version called
                // saveCurrentReadingState() here on every position change
                // — which fires during a fast fling and writes to disk
                // 30+ times per second on a long PDF. The write is async
                // (SharedPreferences.apply()) so it doesn't block the UI,
                // but it still wakes the disk + binder thread constantly.
                // Throttling via a debounce handler is enough — we persist
                // the final position when the scroll settles (see
                // onScrollStateChanged) and on activity pause/stop.
                scheduleThrottledSave()
            } else {
                positionToUseForState = firstVisiblePosition
            }
        }

        private fun updatePageNumberDisplay(position: Int) {
            if (position != NO_POSITION) {
                pageNo.text =
                    context.getString(R.string.pdfView_page_no, position + 1, totalPageCount)
                pageNo.visibility = View.VISIBLE
                if (position == 0) {
                    pageNo.postDelayed({ pageNo.visibility = View.GONE }, 3000)
                }
                statusListener?.onPageChanged(position, totalPageCount)
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                pageNo.postDelayed(runnable, 3000)
                // 2025-08-19 (audit-fix): cancel the throttled save and
                // persist immediately when the scroll settles — the
                // throttle is only for the in-flight case.
                cancelThrottledSave()
                saveCurrentReadingState()
            } else {
                pageNo.removeCallbacks(runnable)
            }
        }
    }

    private fun getAttrs(attrs: AttributeSet?, defStyle: Int) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.PdfRendererView, defStyle, 0)
        setTypeArray(typedArray)
    }

    private fun setTypeArray(typedArray: TypedArray) {
        val engineValue =
            typedArray.getInt(R.styleable.PdfRendererView_pdfView_engine, PdfEngine.INTERNAL.value)
        engine = PdfEngine.values().first { it.value == engineValue }
        showDivider = typedArray.getBoolean(R.styleable.PdfRendererView_pdfView_showDivider, true)
        divider = typedArray.getDrawable(R.styleable.PdfRendererView_pdfView_divider)
        enableLoadingForPages = typedArray.getBoolean(
            R.styleable.PdfRendererView_pdfView_enableLoadingForPages,
            enableLoadingForPages
        )
        val marginDim =
            typedArray.getDimensionPixelSize(R.styleable.PdfRendererView_pdfView_page_margin, 0)
        pageMargin = Rect(marginDim, marginDim, marginDim, marginDim).apply {
            top = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginTop,
                top
            )
            left = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginLeft,
                left
            )
            right = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginRight,
                right
            )
            bottom = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginBottom,
                bottom
            )
        }
        disableScreenshots =
            typedArray.getBoolean(R.styleable.PdfRendererView_pdfView_disableScreenshots, false)
        applyScreenshotSecurity()
        typedArray.recycle()
    }

    private fun applyScreenshotSecurity() {
        if (disableScreenshots) {
            // Disables taking screenshots and screen recording
            (context as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun closePdfRender() {
        // Best-effort: persist current page before tearing down the renderer.
        // 2025-08-19 (audit-fix): cancel any pending throttled save first
        // so we don't fire a save AFTER the renderer is torn down (which
        // would NPE on the layout-manager lookup).
        cancelThrottledSave()
        try { saveCurrentReadingState() } catch (_: Throwable) {}
        if (pdfRendererCoreInitialised) {
            pdfRendererCore.closePdfRender()
            pdfRendererCoreInitialised = false
        }
    }

}
