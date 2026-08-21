package org.horizontal.tella.mobile.views.fragment.googledrive.viewpagerfragments

import androidx.fragment.app.Fragment
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.DRAFT_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.OUTBOX_LIST_PAGE_INDEX

class GoogleDriveFragmentProvider : FragmentProvider {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            DRAFT_LIST_PAGE_INDEX -> DraftsGoogleDriveFragment()
            OUTBOX_LIST_PAGE_INDEX -> OutboxGoogleDriveFragment()
            else -> SubmittedGoogleDriveFragment()
        }
    }
}