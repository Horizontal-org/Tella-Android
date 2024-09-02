package rs.readahead.washington.mobile.views.fragment.main_connexions.fragment

import androidx.fragment.app.Fragment
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider

class ReportsFragmentProvider : FragmentProvider {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DraftsReportFragment()
            1 -> OutboxReportsFragment()
            else -> SubmittedReportsFragment()
        }
    }
}