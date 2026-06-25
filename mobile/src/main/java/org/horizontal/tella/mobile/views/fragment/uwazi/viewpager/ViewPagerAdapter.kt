package org.horizontal.tella.mobile.views.fragment.uwazi.viewpager

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.horizontal.tella.mobile.views.fragment.connections.ConnectionTab
import org.horizontal.tella.mobile.views.fragment.uwazi.*

val TEMPLATES_LIST_PAGE_INDEX = ConnectionTab.uwaziPageIndex(ConnectionTab.TEMPLATES)
val DRAFT_LIST_PAGE_INDEX = ConnectionTab.uwaziPageIndex(ConnectionTab.DRAFTS)
val OUTBOX_LIST_PAGE_INDEX = ConnectionTab.uwaziPageIndex(ConnectionTab.OUTBOX)
val SUBMITTED_LIST_PAGE_INDEX = ConnectionTab.uwaziPageIndex(ConnectionTab.SUBMITTED)

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    /**
     * Mapping of the ViewPager page indexes to their respective Fragments
     */
    private val tabFragmentsCreators: Map<Int, () -> Fragment> = mapOf(
        TEMPLATES_LIST_PAGE_INDEX to { TemplatesUwaziFragment() },
        DRAFT_LIST_PAGE_INDEX to { DraftsUwaziFragment() },
        OUTBOX_LIST_PAGE_INDEX to { OutboxUwaziFragment() },
        SUBMITTED_LIST_PAGE_INDEX to { SubmittedUwaziFragment() }
    )

    override fun getItemCount() = tabFragmentsCreators.size

    override fun createFragment(position: Int): Fragment {
        return tabFragmentsCreators[position]?.invoke() ?: throw IndexOutOfBoundsException()
    }

}
