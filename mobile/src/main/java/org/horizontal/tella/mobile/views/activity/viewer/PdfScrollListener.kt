package org.horizontal.tella.mobile.views.activity.viewer

import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.horizontal.pdfviewer.PdfRendererView
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.util.show

/**
 * Hides/shows the toolbar based on scroll direction.
 *
 * 2025-08-20 (audit-fix rev 8): CRITICAL fix for the "PDF opens in
 * landscape, works for 1 sec, then reverts to portrait" bug.
 *
 * ## Root cause
 *
 * The previous version reacted to `SCROLL_STATE_SETTLING` — which fires
 * both for user drags AND for programmatic scrolls (e.g. the
 * `recyclerView.scrollToPosition(...)` and
 * `lm.scrollToPositionWithOffset(...)` calls inside
 * `PdfRendererView.init()` and `restoreFromPersistentState()`).
 *
 * The 300 ms + 500 ms postDelayed handlers in `PdfRendererView` fire
 * after the activity opens, triggering a programmatic scroll. That
 * scroll enters `SCROLL_STATE_SETTLING` and this listener re-wrote
 * `pdfView.layoutParams` margins to `pdfTopMargin` — which (combined
 * with `android:animateLayoutChanges="true"` on the activity layout)
 * animated the PDF view back to a portrait-era layout over ~300 ms.
 * The user saw "1 sec then back to original state".
 *
 * ## Fix
 *
 * 1. Only react to `SCROLL_STATE_SETTLING` when the user has actually
 *    dragged (`totalDy != 0` OR `scrollDirection != directionNone`).
 *    Programmatic scrolls from `scrollToPosition*` have `dy == 0` on
 *    every `onScrolled` call, so `totalDy` stays at whatever it was
 *    before — but `scrollDirection` is `directionNone` because `dy == 0`
 *    maps to `directionNone` in `onScrolled`. So gating on
 *    `scrollDirection != directionNone` is enough to filter out
 *    programmatic scrolls.
 *
 * 2. Also guard with a `userHasScrolled` flag that flips to true only
 *    when `onScrolled` receives a non-zero `dy`. This is belt-and-
 *    braces: programmatic `scrollToPositionWithOffset(0, -offsetPx)`
 *    from `restoreFromPersistentState` fires `onScrolled` with `dy == 0`,
 *    so the flag stays false and the margin rewrite is skipped.
 *
 * 3. The activity layout's `android:animateLayoutChanges` is now `false`
 *    (see `activity_pdf_reader.xml`) so even if a margin rewrite DOES
 *    happen, it's instant instead of animated — no "1 sec" transition.
 */
class PdfScrollListener(
    private val toolbar: Toolbar,
    private val pdfView: PdfRendererView,
    private val pdfTopMargin: Int
) : RecyclerView.OnScrollListener() {
    private val directionNone = -1
    private val directionUp = 0
    private val directionDown = 1
    private var totalDy = 0

    private var scrollDirection = directionNone
    private var listStatus = RecyclerView.SCROLL_STATE_IDLE

    /** True only after the user has actually dragged (non-zero dy).
     *  Programmatic scrolls (scrollToPosition / scrollToPositionWithOffset)
     *  never set this to true because they fire onScrolled with dy == 0. */
    private var userHasScrolled = false

    /** 2026-08-20 (audit-fix rev 10): when the user is zoomed in, the
     *  canvas transform (translate + scale) is applied at draw time, not
     *  at scroll time — the RecyclerView's scroll position doesn't change
     *  when the user pans a zoomed page, so onScrolled never fires and
     *  the toolbar gets stuck hidden. We set this flag from the zoom
     *  listener in PDFReaderActivity to prevent the toolbar from being
     *  hidden while zoomed. */
    var isZoomed: Boolean = false

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        listStatus = newState

        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            scrollDirection = directionNone
        }

        // 2026-08-20 (audit-fix rev 10): if zoomed in, NEVER hide the
        // toolbar. The user needs it visible to access the rotate button,
        // share, etc. The zoom listener in PDFReaderActivity already
        // forces it visible when zoom changes; this guard prevents the
        // scroll listener from hiding it during a fling while zoomed.
        if (isZoomed) return

        // 2025-08-20 (audit-fix rev 8): only rewrite margins on USER-initiated
        // settling, not programmatic. The `userHasScrolled` guard filters out
        // the postDelayed scrollToPosition calls from PdfRendererView.
        if (newState == RecyclerView.SCROLL_STATE_SETTLING && userHasScrolled) {
            if (getDragDirection() == directionDown || isOnTop()) {
                toolbar.show()
                val param = pdfView.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, pdfTopMargin, 0, 0)
                pdfView.layoutParams = param
                toolbar.outlineProvider = null

            } else if (getDragDirection() == directionUp) {
                toolbar.hide()
                val param = pdfView.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, 0, 0, 0)
                pdfView.layoutParams = param
                pdfView.outlineProvider = null
            }
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        // 2025-08-20 (audit-fix rev 8): only mark userHasScrolled when dy != 0.
        // Programmatic scrolls from scrollToPosition* have dy == 0.
        if (dy != 0) {
            userHasScrolled = true
        }
        this.totalDy += dy
        scrollDirection = when {
            dy > 0 -> directionUp
            dy < 0 -> directionDown
            else -> directionNone
        }
    }

    private fun isOnTop(): Boolean {
        return totalDy == 0
    }

    private fun getDragDirection(): Int {
        if (listStatus != RecyclerView.SCROLL_STATE_SETTLING) {
            return directionNone
        }

        return when (scrollDirection) {
            directionNone -> if (totalDy == 0) {
                directionDown  // drag down from top
            } else {
                directionUp  // drag up from bottom
            }

            directionUp -> directionUp
            directionDown -> directionDown
            else -> directionNone
        }
    }
}
