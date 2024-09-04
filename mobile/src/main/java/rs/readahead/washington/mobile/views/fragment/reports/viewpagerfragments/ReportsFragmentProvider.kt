package rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments

import androidx.fragment.app.Fragment
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider

const val DRAFT_LIST_PAGE_INDEX = 0
const val OUTBOX_LIST_PAGE_INDEX = 1
const val SUBMITTED_LIST_PAGE_INDEX = 2

class ReportsFragmentProvider : FragmentProvider {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            DRAFT_LIST_PAGE_INDEX -> DraftsReportsFragment()
            OUTBOX_LIST_PAGE_INDEX -> OutboxReportsFragment()
            else -> SubmittedReportsFragment()
        }
    }
}