package com.horizontal.pdfviewer.annotations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.horizontal.pdfviewer.R
import java.util.UUID
import kotlin.math.abs

/**
 * Transparent overlay placed on top of each rendered PDF page.
 *
 * 2025-08-19 (audit revision 6 — complete rewrite):
 *
 *  - Tap-to-create highlight (no drag — no crash with pinch-zoom).
 *  - Tap-to-create sticky note (pushpin marker, no "i" glyph).
 *  - Tap-to-edit existing annotations (hit-test on DOWN in OFF mode).
 *  - Long-press on empty space → "Copy text" context menu.
 *  - Highlight: separate width + height multipliers (from style picker).
 *  - Sticky note: size multiplier (from style picker).
 *  - Sticky note rendering uses annotation's own size (not fixed).
 *  - PinchZoomRecyclerView crash fix (pointerIndex bounds check) is in
 *    PinchZoomRecyclerView.kt, not here — this overlay just doesn't
 *    consume ACTION_MOVE in annotation mode so pinch-zoom works.
 */
class PdfAnnotationOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ----- Public state -----

    var pageIndex: Int = 0
        set(value) { field = value; invalidate() }

    var annotations: List<PdfAnnotation> = emptyList()
        set(value) { field = value; invalidate() }

    var annotationMode: AnnotationMode = AnnotationMode.OFF
        set(value) { field = value; invalidate() }

    var listener: AnnotationListener? = null

    var highlightColor: Int = DEFAULT_HIGHLIGHT_COLOR
    var stickyNoteColor: Int = DEFAULT_STICKY_COLOR

    // 2025-08-19 (audit rev6): separate width + height multipliers for highlights.
    var highlightWidthMultiplier: Float = 0.35f   // fraction of page width (0.20/0.35/0.50)
    var highlightHeightMultiplier: Float = 1.0f   // line height multiplier (0.7/1.0/1.5)
    var stickyNoteSizeMultiplier: Float = 1.0f

    // ----- Drawing state -----

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = HIGHLIGHT_ALPHA
    }
    private val stickyCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = DEFAULT_STICKY_COLOR
    }
    private val stickyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2f
    }
    private val tapHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.pdf_annotation_drag_border)
        strokeWidth = 4f
    }

    private val stickyMarkerSizePx: Float by lazy { 32f * resources.displayMetrics.density }

    // ----- Touch state -----

    private var offModePendingTap: PdfAnnotation? = null
    private var offModeDownX: Float = 0f
    private var offModeDownY: Float = 0f
    private var lastTapX: Float = -1f
    private var lastTapY: Float = -1f

    /** Host-side listener for "user tapped an existing annotation in OFF mode". */
    var offModeTapListener: OffModeTapListener? = null

    /** Host-side listener for long-press on empty space. */
    var longPressListener: LongPressListener? = null

    fun interface OffModeTapListener {
        fun onAnnotationTapped(annotation: PdfAnnotation)
    }
    fun interface LongPressListener {
        fun onLongPress(pageIndex: Int, x: Float, y: Float)
    }

    init {
        isLongClickable = true
        setOnLongClickListener {
            if (offModePendingTap == null && annotationMode == AnnotationMode.OFF) {
                longPressListener?.onLongPress(pageIndex, offModeDownX, offModeDownY)
                true
            } else false
        }
    }

    // ----- Touch handling -----

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (annotationMode == AnnotationMode.OFF) {
            return handleOffModeTap(event)
        }
        // In annotation mode: consume DOWN, create on UP, ignore MOVE (pinch works)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                lastTapX = event.x; lastTapY = event.y; invalidate()
                if (annotationMode == AnnotationMode.HIGHLIGHT) {
                    commitHighlightAt(event.x, event.y)
                } else if (annotationMode == AnnotationMode.STICKY_NOTE) {
                    commitStickyNoteAt(event.x, event.y)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return false
    }

    private fun handleOffModeTap(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = hitTestStickyNote(event.x, event.y) ?: hitTestHighlight(event.x, event.y)
                offModePendingTap = hit
                offModeDownX = event.x; offModeDownY = event.y
                return hit != null
            }
            MotionEvent.ACTION_UP -> {
                val pending = offModePendingTap
                offModePendingTap = null
                if (pending != null) {
                    performClick()
                    offModeTapListener?.onAnnotationTapped(pending)
                    return true
                }
                return false
            }
            MotionEvent.ACTION_CANCEL -> { offModePendingTap = null; return false }
        }
        return false
    }

    // ----- Annotation creation -----

    private fun commitHighlightAt(rawX: Float, rawY: Float) {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val lineH = 24f * resources.displayMetrics.density * highlightHeightMultiplier
        val halfW = highlightWidthMultiplier  // fraction of page width
        val cx = (rawX / w).coerceIn(halfW, 1f - halfW)
        val cy = (rawY / h).coerceIn(0f, 1f)
        listener?.onAnnotationCreated(PdfAnnotation(
            id = UUID.randomUUID().toString(),
            page = pageIndex,
            type = PdfAnnotation.Type.HIGHLIGHT,
            x = cx - halfW,
            y = (cy - (lineH / 2f) / h).coerceAtLeast(0f),
            width = halfW * 2f,
            height = lineH / h,
            text = "",
            color = highlightColor,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))
    }

    private fun commitStickyNoteAt(rawX: Float, rawY: Float) {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val markerSize = stickyMarkerSizePx * stickyNoteSizeMultiplier
        listener?.onStickyNoteRequested(PdfAnnotation(
            id = UUID.randomUUID().toString(),
            page = pageIndex,
            type = PdfAnnotation.Type.STICKY_NOTE,
            x = (rawX / w).coerceIn(0f, 1f),
            y = (rawY / h).coerceIn(0f, 1f),
            width = markerSize / w,
            height = markerSize / h,
            text = "",
            color = stickyNoteColor,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))
    }

    // ----- Drawing -----

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Highlights
        annotations.filter { it.type == PdfAnnotation.Type.HIGHLIGHT && it.page == pageIndex }.forEach { a ->
            highlightPaint.color = a.color; highlightPaint.alpha = HIGHLIGHT_ALPHA
            canvas.drawRect(RectF(a.x * w, a.y * h, (a.x + a.width) * w, (a.y + a.height) * h), highlightPaint)
        }

        // Sticky notes — pushpin style (filled circle + tail + white border)
        annotations.filter { it.type == PdfAnnotation.Type.STICKY_NOTE && it.page == pageIndex }.forEach { a ->
            val cx = (a.x * w) + (a.width * w) / 2f
            val cy = (a.y * h) + (a.height * h) / 2f
            val r = (a.width * w).coerceAtLeast(8f * resources.displayMetrics.density) / 2f
            stickyCirclePaint.color = a.color
            // Tail
            canvas.drawPath(android.graphics.Path().apply {
                moveTo(cx - r * 0.4f, cy + r * 0.4f)
                lineTo(cx + r * 0.4f, cy + r * 0.4f)
                lineTo(cx - r * 0.8f, cy + r * 1.2f)
                close()
            }, stickyCirclePaint)
            // Circle
            canvas.drawCircle(cx, cy, r, stickyCirclePaint)
            // White border
            canvas.drawCircle(cx, cy, r, stickyBorderPaint)
        }

        // Tap hint ring
        if (annotationMode != AnnotationMode.OFF && lastTapX >= 0 && lastTapY >= 0) {
            canvas.drawCircle(lastTapX, lastTapY, 24f * resources.displayMetrics.density, tapHintPaint)
        }
    }

    // ----- Hit testing -----

    fun hitTestStickyNote(x: Float, y: Float): PdfAnnotation? {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return null
        return annotations.firstOrNull { a ->
            a.type == PdfAnnotation.Type.STICKY_NOTE && a.page == pageIndex && run {
                val markerSize = (a.width * w).coerceAtLeast(8f * resources.displayMetrics.density)
                val r = markerSize / 2f
                val cx = (a.x * w) + (a.width * w) / 2f
                val cy = (a.y * h) + (a.height * h) / 2f
                abs(cx - x) <= r * 1.5f && abs(cy - y) <= r * 1.5f
            }
        }
    }

    fun hitTestHighlight(x: Float, y: Float): PdfAnnotation? {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return null
        return annotations.firstOrNull { a ->
            a.type == PdfAnnotation.Type.HIGHLIGHT && a.page == pageIndex && run {
                x in (a.x * w)..((a.x + a.width) * w) && y in (a.y * h)..((a.y + a.height) * h)
            }
        }
    }

    enum class AnnotationMode { OFF, HIGHLIGHT, STICKY_NOTE }

    interface AnnotationListener {
        fun onAnnotationCreated(annotation: PdfAnnotation)
        fun onStickyNoteRequested(annotation: PdfAnnotation)
    }

    companion object {
        const val DEFAULT_HIGHLIGHT_COLOR: Int = 0xFFFFFF00.toInt()
        const val DEFAULT_STICKY_COLOR: Int = 0xFFE54A2D.toInt()
        const val HIGHLIGHT_ALPHA: Int = 90
    }
}
