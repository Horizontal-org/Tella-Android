package rs.readahead.washington.mobile.views.fragment.dropbox.viewpagerfragments

import androidx.fragment.app.activityViewModels
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.dropbox.DropBoxViewModel
import rs.readahead.washington.mobile.views.fragment.googledrive.GoogleDriveViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.MainReportFragment

internal class DropBoxTabsFragment : MainReportFragment() {

    override val viewModel by activityViewModels<DropBoxViewModel>()

    override fun getFragmentProvider(): FragmentProvider {
        return DropboxFragmentProvider()
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.dropbox)
    }

    override fun navigateToNewReportScreen() {
        this.navManager().navigateFromGoogleDriveScreenToNewGoogleDriveScreen()
    }

}