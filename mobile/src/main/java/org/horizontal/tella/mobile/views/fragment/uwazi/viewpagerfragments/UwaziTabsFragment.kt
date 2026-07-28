package org.horizontal.tella.mobile.views.fragment.uwazi.viewpagerfragments

import androidx.fragment.app.activityViewModels
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.MainReportFragment
import org.horizontal.tella.mobile.views.fragment.uwazi.UwaziViewModel

internal class UwaziTabsFragment : MainReportFragment() {

    override val viewModel by activityViewModels<UwaziViewModel>()

    override val listTabOffset: Int = UWAZI_LIST_TAB_OFFSET

    override fun getFragmentProvider(): FragmentProvider {
        return UwaziFragmentProvider()
    }

    override fun getToolbarTitle(): String {
        return getString(R.string.fragment_uwazi)
    }

    override fun getTabTitles(): List<String> = listOf(
        getString(R.string.Uwazi_Templates_TabTitle),
        getString(R.string.collect_draft_tab_title),
        getString(R.string.collect_outbox_tab_title),
        getString(R.string.collect_sent_tab_title)
    )

    override fun getNewButtonText(): Int = R.string.Uwazi_Dowload_Temapltes_Header_Title

    override fun navigateToNewReportScreen() {
        this.navManager().navigateFromUwaziScreenToDownloadScreen()
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_uwazi
    }

    override fun getEmptyMessageForTab(position: Int): String? =
        if (position == TEMPLATES_TAB_INDEX) {
            getString(R.string.Uwazi_Templates_Empty_Description)
        } else {
            super.getEmptyMessageForTab(position)
        }
}
