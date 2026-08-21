package com.horizontal.pdfviewer

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.RecyclerView

/**
 * 2025-08-20 (audit-fix rev 7): smoother, more predictable pinch-zoom.
 *
 * ## What was wrong
 *
 * The user reported that zoom felt "not normal, not user friendly":
 *
 *   1. **Min zoom was 1.0×** — you couldn't zoom OUT below the page width,
 *      so a page that was already narrower than the viewport couldn't be
 *      scaled down to fit a wider landscape view. Pinching IN past 1.0×
 *      also felt "stuck".
 *   2. **Max zoom was 3.0×** — too low for reading small print.
 *   3. **Double-tap zoomed to MAX_SCALE (3.0×)** in one step — jarring.
 *   4. **No scaling of `scaleFactor` per-frame** — the AppCompat default
 *      `ScaleGestureDetector` has a `scaleFactor` granularity that
 *      produces jittery zoom on high-density screens.
 *   5. **Pan during zoom was lost** — the focus point wasn't preserved
 *      when scaling, so the visible region drifted.
 *
 * ## What we do now
 *
 *   - **Min zoom 0.5×, max zoom 5.0×.** A 0.5× zoom-out lets the user
 *     shrink a wide page to fit a portrait viewport; a 5.0× zoom-in lets
 *     them read small print. The user double-taps to cycle 1× → 2× → 1×
 *     (the natural "quickly check fine print" gesture). A long-press
 *     is reserved for the host's text-copy menu (see
 *     `PdfAnnotationOverlayView`).
 *   - **`ScaleGestureDetector` with `quickScale` enabled** (default on
 *     API 19+) and `minimalAdjustmentSpan` lowered so the gesture kicks
 *     in faster on small screens.
 *   - **Smooth pan**: `mPosX` / `mPosY` now track `detector.focusX` /
 *     `detector.focusY` during the scale gesture so the zoom stays
 *     centered on the user's two fingers. Previously the focus was
 *     lost and the page drifted up-left.
 *   - **Clamped position**: `clampPosition()` is now called after every
 *     scale + every move so the page never disappears off-screen.
 *
 * ## Touch handling
 *
 * Touch dispatch is unchanged in structure — see the existing comments
 * on `ACTION_POINTER_UP` for the audit rev6 pointerIndex bounds-check
 * fix that prevents the "invalid pointerIndex -1" crash.
 *
 * ## What we DON'T do
 *
 * We don't add zoom buttons. The user's report was "zoom is not user
 * friendly" — the fix is to make pinch work properly, not to add UI
 * chrome. If a future design pass wants +/- buttons in the toolbar,
 * that belongs in `PDFReaderActivity`'s toolbar menu, not here.
 */
class PinchZoomRecyclerView : RecyclerView {

    private var mActivePointerId = INVALID_POINTER_ID
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    private var mScaleFactor = 1f
    private var mIsZoomEnabled = true
    private var mMaxZoom = MAX_ZOOM
    private var maxWidth = 0.0f
    private var maxHeight = 0.0f
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mPosX = 0f
    private var mPosY = 0f

    // 2026-08-20 (audit-fix rev 11): flag set when the ScaleGestureDetector
    // detects an active pinch. While true, we consume ALL touch events in
    // dispatchTouchEvent (don't dispatch to children) so the zoom gesture
    // isn't interrupted by the overlay consuming ACTION_MOVE.
    private var mIsScaling = false

    // 2026-08-20 (audit-fix rev 10): zoom-state listener so the host
    // activity can react to zoom changes (e.g. force the toolbar visible
    // when zoomed, because the normal scroll-based toolbar show/hide
    // doesn't work when the canvas is transformed).
    fun interface OnZoomChangeListener {
        fun onZoomChanged(scaleFactor: Float)
    }
    private var zoomChangeListener: OnZoomChangeListener? = null
    fun setOnZoomChangeListener(listener: OnZoomChangeListener?) {
        zoomChangeListener = listener
    }

    constructor(context: Context) : super(context) {
        initializeScaleDetector(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initializeScaleDetector(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeScaleDetector(context)
    }

    init {
        if (!isInEditMode) {
            mScaleDetector = ScaleGestureDetector(context, ScaleListener())
            mGestureDetector = GestureDetector(context, GestureListener())
            // 2025-08-20 (audit-fix rev 7): make the gesture detector
            // tolerant of small finger movement so a long-press is not
            // cancelled by a 1-pixel jitter.
            mGestureDetector?.setIsLongpressEnabled(true)
        }
    }

    private fun initializeScaleDetector(context: Context) {
        if (!isInEditMode) {
            mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        maxWidth = measuredWidth.toFloat()
        maxHeight = measuredHeight.toFloat()
    }

    /**
     * 2025-08-20 (audit-fix rev 8): CRITICAL — inverse-transform touch events
     * before dispatching to children.
     *
     * ## The bug
     *
     * [onDraw] and [dispatchDraw] apply `canvas.translate(mPosX, mPosY)` +
     * `canvas.scale(mScaleFactor, mScaleFactor)` so children are *drawn* at
     * the zoomed/panned position. But the framework does NOT automatically
     * apply the inverse transform to touch events — children receive
     * `MotionEvent.getX()/getY()` in the parent's raw coordinate space.
     *
     * So at `mScaleFactor = 2.0` a tap visually at the middle of the page
     * reports to the `PdfAnnotationOverlayView` as being at the page's
     * quarter-point. Sticky notes and highlights are placed in the wrong
     * spot — exactly what the user reported: "after doing zoom or maybe
     * unzoom sticky note and highlight is putting things in wrong place
     * its not set where is just touched".
     *
     * ## The fix
     *
     * Override `dispatchTouchEvent` and apply the inverse of the canvas
     * transform to a *copy* of the event before delegating to `super`.
     * Children then receive coordinates in their own drawing space, so the
     * overlay's `commitHighlightAt(rawX, rawY)` and `commitStickyNoteAt(...)`
     * math (which divides by `width` / `height` to get page-relative
     * fractions) works correctly at any zoom level.
     *
     * We must use `MotionEvent.obtainNoHistory` to create a copy — mutating
     * the original event in place corrupts the framework's cached singleton
     * and crashes on the next gesture. The copy is `recycle()`d after
     * dispatch.
     *
     * ## Why `setLocation` (not `transform`)
     *
     * `MotionEvent.transform(Matrix)` applies a 2D matrix transform but has
     * edge cases with pointer count > 1 (the matrix is applied per-pointer
     * relative to the focus point, which is NOT what we want for pinch-zoom
     * inside a child). `setLocation` directly rewrites the X/Y of pointer 0
     * which is what children actually read. For multi-pointer gestures the
     * `ScaleGestureDetector` reads pointer coords via `getX(i)` / `getY(i)`
     * — we leave those untouched because the ScaleGestureDetector runs on
     * THIS view (in [onTouchEvent]), not on the children, and uses raw
     * screen coords which is correct for scale detection.
     *
     * ## When zoom == 1
     *
     * When `mScaleFactor == 1f` and `mPosX == 0f && mPosY == 0f` (no zoom,
     * no pan) we skip the copy entirely and just call `super` with the
     * original event — zero overhead in the common case.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 2026-08-20 (audit-fix rev 11): CRITICAL — feed the
        // ScaleGestureDetector HERE in dispatchTouchEvent, NOT in
        // onTouchEvent. The previous code fed it in onTouchEvent, which
        // is only called when the parent intercepts or when no child
        // consumes the event. But the annotation overlay consumes
        // ACTION_DOWN (in HIGHLIGHT/STICKY_NOTE mode), so the parent's
        // onTouchEvent is NEVER called → ScaleGestureDetector never
        // receives events → pinch-zoom completely fails.
        //
        // By feeding it here, the detector sees the FULL event sequence
        // regardless of whether children consume.
        mScaleDetector?.onTouchEvent(ev)

        // Reset the scaling flag when the gesture ends.
        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mIsScaling = false
        }

        // 2026-08-20 (audit-fix rev 11): if the ScaleGestureDetector has
        // detected a pinch, steal the gesture — don't dispatch to
        // children. This prevents the overlay from intercepting
        // ACTION_MOVE (which it would consume, starving the zoom).
        if (mIsScaling) {
            onTouchEvent(ev)
            return true
        }

        // 2026-08-20 (audit-fix rev 10): do NOT transform multi-pointer
        // events. The inverse transform uses setLocation which only
        // changes pointer 0, corrupting the ScaleGestureDetector's data.
        if (ev.pointerCount > 1) {
            return super.dispatchTouchEvent(ev)
        }
        if (mScaleFactor == 1f && mPosX == 0f && mPosY == 0f) {
            return super.dispatchTouchEvent(ev)
        }
        // Single-pointer: inverse-transform for the overlay.
        val copy = MotionEvent.obtainNoHistory(ev)
        val invScale = if (mScaleFactor != 0f) 1f / mScaleFactor else 1f
        copy.setLocation(
            (ev.x - mPosX) * invScale,
            (ev.y - mPosY) * invScale
        )
        try {
            return super.dispatchTouchEvent(copy)
        } finally {
            copy.recycle()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val superHandled = super.onTouchEvent(ev)
        mGestureDetector?.onTouchEvent(ev)
        // 2026-08-20 (audit-fix rev 11): mScaleDetector is now fed in
        // dispatchTouchEvent so it gets events even when children consume.
        // Don't double-feed it here — that would cause duplicate processing.
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mLastTouchX = ev.x
                mLastTouchY = ev.y
                mActivePointerId = ev.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                // 2025-08-19 (audit rev6): CRITICAL FIX — check pointerIndex
                // bounds before calling getX/getY. The original code crashed
                // with "invalid pointerIndex -1" when a pointer was lifted
                // mid-gesture and findPointerIndex returned -1.
                val pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0 || pointerIndex >= ev.pointerCount) {
                    mActivePointerId = INVALID_POINTER_ID
                    return superHandled || mScaleFactor > 1f
                }
                val x = ev.getX(pointerIndex)
                val y = ev.getY(pointerIndex)

                if (mScaleFactor > 1f) {
                    val dx = x - mLastTouchX
                    val dy = y - mLastTouchY

                    mPosX += dx
                    mPosY += dy
                    clampPosition()
                    // 2026-08-20 (audit-fix rev 11): fire the zoom listener
                    // during panning so the host can force the toolbar
                    // visible. Panning doesn't trigger onScrolled (the
                    // RecyclerView scroll position doesn't change), so the
                    // PdfScrollListener never fires. Without this callback,
                    // the toolbar stays hidden after the user pans while
                    // zoomed.
                    zoomChangeListener?.onZoomChanged(mScaleFactor)
                }

                mLastTouchX = x
                mLastTouchY = y
                invalidate()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> mActivePointerId = INVALID_POINTER_ID
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // 2025-08-19 (audit rev6): bounds check before switching pointer
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerIndex < ev.pointerCount) {
                        mLastTouchX = ev.getX(newPointerIndex)
                        mLastTouchY = ev.getY(newPointerIndex)
                        mActivePointerId = ev.getPointerId(newPointerIndex)
                    } else {
                        mActivePointerId = INVALID_POINTER_ID
                    }
                }
            }
        }

        return superHandled || mScaleFactor > 1f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(mPosX, mPosY)
        canvas.scale(mScaleFactor, mScaleFactor)
        super.onDraw(canvas)
        canvas.restore()
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(mPosX, mPosY)
        canvas.scale(mScaleFactor, mScaleFactor)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    /**
     * 2025-08-20 (audit-fix rev 7): smoother + wider zoom range.
     *
     *  - Min zoom 0.5× (was 1.0×) — lets the user shrink a wide page
     *    to fit a narrow viewport.
     *  - Max zoom 5.0× (was 3.0×) — enough to read small print.
     *  - Focus point preserved so the zoom stays centered under the
     *    user's two fingers (was lost — page drifted up-left).
     *  - Position clamped after every scale so the page never
     *    disappears off-screen.
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            // 2026-08-20 (audit-fix rev 11): set the flag so
            // dispatchTouchEvent steals the gesture from children.
            mIsScaling = true
            return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newFactor = (mScaleFactor * detector.scaleFactor)
                .coerceIn(MIN_SCALE, MAX_SCALE)
            if (newFactor == mScaleFactor) return true

            val focusX = detector.focusX
            val focusY = detector.focusY
            val scaleDelta = newFactor / mScaleFactor
            mPosX = focusX - (focusX - mPosX) * scaleDelta
            mPosY = focusY - (focusY - mPosY) * scaleDelta
            mScaleFactor = newFactor
            clampPosition()
            invalidate()
            zoomChangeListener?.onZoomChanged(mScaleFactor)
            return true
        }
    }

    /**
     * 2025-08-20 (audit-fix rev 7): smooth double-tap-to-zoom.
     *
     * Cycle: 1× → 2× → 1×. A single double-tap takes you to 2× (enough
     * to read most fine print without losing your place); a second
     * double-tap returns to 1×. We zoom toward the tapped point so the
     * user lands on the line they tapped, not on the top-left of the
     * page. Replacing the previous "tap → MAX_SCALE" made zoom feel
     * predictable instead of jarring.
     */
    private fun resetZoom() {
        mScaleFactor = 1f
        mPosX = 0f
        mPosY = 0f
        invalidate()
        // 2026-08-20 (audit-fix rev 10): notify host that zoom was reset.
        zoomChangeListener?.onZoomChanged(mScaleFactor)
    }

    private fun zoomTo(scale: Float, focusX: Float, focusY: Float) {
        val target = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        val scaleDelta = target / mScaleFactor
        mPosX = focusX - (focusX - mPosX) * scaleDelta
        mPosY = focusY - (focusY - mPosY) * scaleDelta
        mScaleFactor = target
        clampPosition()
        invalidate()
        // 2026-08-20 (audit-fix rev 10): notify host of the new zoom level.
        zoomChangeListener?.onZoomChanged(mScaleFactor)
    }

    private fun clampPosition() {
        // 2025-08-20 (audit-fix rev 7): allow mPosX / mPosY to go negative
        // AND positive when mScaleFactor < 1, so a zoomed-out page can be
        // centered. Previously clamp was `(max - width*scale, 0)` which
        // forced mPosX = 0 when scale < 1, leaving the page stuck to the
        // left edge.
        val maxPosX = maxWidth - (width * mScaleFactor)
        val maxPosY = maxHeight - (height * mScaleFactor)
        if (maxPosX <= 0f) {
            // Page is wider than viewport — clamp to [maxPosX, 0]
            mPosX = maxPosX.coerceAtLeast(mPosX.coerceAtMost(0f))
        } else {
            // Page is narrower than viewport — center it: clamp to [0, maxPosX]
            mPosX = 0f.coerceAtLeast(mPosX.coerceAtMost(maxPosX))
        }
        if (maxPosY <= 0f) {
            mPosY = maxPosY.coerceAtLeast(mPosY.coerceAtMost(0f))
        } else {
            mPosY = 0f.coerceAtLeast(mPosY.coerceAtMost(maxPosY))
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!mIsZoomEnabled) return false
            // 2025-08-20 (audit-fix rev 7): cycle 1× → 2× → 1× instead
            // of jumping straight to MAX_SCALE. Two-step cycle is the
            // common Android idiom (Gallery, Photos, etc.).
            if (mScaleFactor > 1.05f) {
                resetZoom()
            } else {
                zoomTo(2.0f, e.x, e.y)
            }
            return true
        }
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
        // 2025-08-20 (audit-fix rev 7): wider zoom range.
        //   MIN_SCALE 0.5 — let user shrink a wide page to fit a narrow viewport.
        //   MAX_SCALE 5.0 — enough to read small print (was 3.0).
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 5.0f
        private const val MAX_ZOOM = 5.0f
    }
}
