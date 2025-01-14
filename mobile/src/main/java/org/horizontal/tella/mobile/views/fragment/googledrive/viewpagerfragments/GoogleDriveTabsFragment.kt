package org.horizontal.tella.mobile.views.fragment.googledrive.viewpagerfragments

import androidx.fragment.app.activityViewModels
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.fragment.googledrive.GoogleDriveViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.MainReportFragment

internal class GoogleDriveTabsFragment : MainReportFragment() {

    override val viewModel by activityViewModels<GoogleDriveViewModel>()

    override fun getFragmentProvider(): FragmentProvider {
        return GoogleDriveFragmentProvider()
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.google_drive)
    }

    override fun navigateToNewReportScreen() {
        this.navManager().navigateFromGoogleDriveScreenToNewGoogleDriveScreen()
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_google_drive
    }

}