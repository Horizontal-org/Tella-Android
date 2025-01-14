package rs.readahead.washington.mobile.views.fragment.nextCloud.viewpagerfragments

import androidx.fragment.app.Fragment
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.DRAFT_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.OUTBOX_LIST_PAGE_INDEX

class NextCloudFragmentProvider : FragmentProvider {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            DRAFT_LIST_PAGE_INDEX -> DraftsNextCloudFragment()
            OUTBOX_LIST_PAGE_INDEX -> OutboxNextCloudFragment()
            else -> SubmittedNextCloudFragment()
        }
    }
}