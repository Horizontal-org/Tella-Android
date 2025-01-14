package org.horizontal.tella.mobile.views.fragment.reports.viewpager

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments.DraftsReportsFragment
import org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments.OutboxReportsFragment
import org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments.SubmittedReportsFragment

const val DRAFT_LIST_PAGE_INDEX = 0
const val OUTBOX_LIST_PAGE_INDEX = 1
const val SUBMITTED_LIST_PAGE_INDEX = 2

class ViewPagerAdapter (fragment: Fragment) : FragmentStateAdapter(fragment) {

    /**
     * Mapping of the ViewPager page indexes to their respective Fragments
     */
    private val tabFragmentsCreators: Map<Int, () -> Fragment> = mapOf(
        DRAFT_LIST_PAGE_INDEX to { DraftsReportsFragment() },
        OUTBOX_LIST_PAGE_INDEX to { OutboxReportsFragment() },
        SUBMITTED_LIST_PAGE_INDEX to { SubmittedReportsFragment() }
    )

    override fun getItemCount() = tabFragmentsCreators.size

    override fun createFragment(position: Int): Fragment {
        return tabFragmentsCreators[position]?.invoke() ?: throw IndexOutOfBoundsException()
    }

}
