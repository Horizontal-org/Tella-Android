package org.horizontal.tella.mobile.views.fragment.vault.home

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Home screen scroll contract: the vault list must always stay pinned to the top when
 * content updates or when the user returns to the home tab.
 *
 * Do not change this behavior without updating [HomeScreenScrollTest].
 */
object HomeScreenScroll {

    /** RecyclerView position for the top of the home screen. Must remain 0. */
    const val HOME_SCROLL_POSITION = 0

    /**
     * Schedules a scroll to [HOME_SCROLL_POSITION] on the next layout pass.
     * No-op when [recyclerView] is null.
     */
    fun scrollToTop(recyclerView: RecyclerView?) {
        if (recyclerView == null) return
        recyclerView.post {
            scrollToTopImmediate(recyclerView)
            // ListAdapter may adjust scroll after the first layout pass when items are inserted.
            recyclerView.post { scrollToTopImmediate(recyclerView) }
        }
    }

    private fun scrollToTopImmediate(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(HOME_SCROLL_POSITION, 0)
        } else {
            recyclerView.scrollToPosition(HOME_SCROLL_POSITION)
        }
    }
}
