package rs.readahead.washington.mobile.views.activity.viewer

import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.horizontal.pdfviewer.PdfRendererView
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show

class PdfScrollListener(
    private val toolbar: Toolbar,
    private val pdfView: PdfRendererView,
    private val pdfTopMargin: Int
) : RecyclerView.OnScrollListener() {
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

        if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
            if (getDragDirection() == DIRECTION_DOWN || isOnTop()) {
                toolbar.show()
                val param =
                    pdfView.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, pdfTopMargin, 0, 0)
                pdfView.layoutParams = param
                toolbar.outlineProvider = null

            } else if (getDragDirection() == DIRECTION_UP) {
                toolbar.hide()
                val param =
                    pdfView.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, 0, 0, 0)
                pdfView.layoutParams = param
                pdfView.outlineProvider = null
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
        if (listStatus != RecyclerView.SCROLL_STATE_SETTLING) {
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
}