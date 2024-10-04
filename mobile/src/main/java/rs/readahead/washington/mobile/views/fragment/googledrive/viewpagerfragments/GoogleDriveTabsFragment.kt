package rs.readahead.washington.mobile.views.fragment.googledrive.viewpagerfragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.googledrive.GoogleDriveViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.MainReportFragment

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
}