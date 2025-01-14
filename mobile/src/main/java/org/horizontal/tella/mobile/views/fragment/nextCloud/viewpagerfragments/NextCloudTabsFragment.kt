package org.horizontal.tella.mobile.views.fragment.nextCloud.viewpagerfragments

import androidx.fragment.app.activityViewModels
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.MainReportFragment
import org.horizontal.tella.mobile.views.fragment.nextCloud.NextCloudViewModel

internal class NextCloudTabsFragment : MainReportFragment() {

    override val viewModel by activityViewModels<NextCloudViewModel>()

    override fun getFragmentProvider(): FragmentProvider {
        return NextCloudFragmentProvider()
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.NextCloud)
    }

    override fun navigateToNewReportScreen() {
        this.navManager().navigateFromNextCloudScreenToNewNextCloudScreen()
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_nextcloud
    }

}