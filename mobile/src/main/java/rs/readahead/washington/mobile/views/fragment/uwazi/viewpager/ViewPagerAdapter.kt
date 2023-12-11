package rs.readahead.washington.mobile.views.fragment.uwazi.viewpager

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.*

const val TEMPLATES_LIST_PAGE_INDEX = 0
const val DRAFT_LIST_PAGE_INDEX = 1
const val OUTBOX_LIST_PAGE_INDEX = 2
const val SUBMITTED_LIST_PAGE_INDEX = 3

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
