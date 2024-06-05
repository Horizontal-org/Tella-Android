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
    private val directionNone = -1
    private val directionUp = 0
    private val directionDown = 1
    private var totalDy = 0

    private var scrollDirection = directionNone
    private var listStatus = RecyclerView.SCROLL_STATE_IDLE

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        listStatus = newState

        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            scrollDirection = directionNone
        }

        if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
            if (getDragDirection() == directionDown || isOnTop()) {
                toolbar.show()
                val param =
                    pdfView.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(0, pdfTopMargin, 0, 0)
                pdfView.layoutParams = param
                toolbar.outlineProvider = null

            } else if (getDragDirection() == directionUp) {
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