package rs.readahead.washington.mobile.views.fragment.dropbox.viewpagerfragments

import androidx.fragment.app.Fragment
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.DRAFT_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.OUTBOX_LIST_PAGE_INDEX

class DropboxFragmentProvider : FragmentProvider {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            DRAFT_LIST_PAGE_INDEX -> DraftsDropBoxFragment()
            OUTBOX_LIST_PAGE_INDEX -> OutboxDropBoxFragment()
            else -> SubmittedDropBoxFragment()
        }
    }
}

