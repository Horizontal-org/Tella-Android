package org.horizontal.tella.mobile.views.fragment.uwazi.viewpagerfragments

import android.view.View
import androidx.fragment.app.activityViewModels
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.DRAFT_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.MainReportFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.OUTBOX_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SUBMITTED_LIST_PAGE_INDEX
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

    override fun navigateToNewReportScreen() {
        this.navManager().navigateFromUwaziScreenToDownloadScreen()
    }

    override fun configurePrimaryActionButton() {
        binding.newReportBtn.visibility = View.GONE
        binding.fabButton.apply {
            visibility = View.VISIBLE
            setOnClickListener { navigateToNewReportScreen() }
        }
    }

    override fun getEmptyMessageIcon(): Int {
        return R.drawable.ic_uwazi
    }

    override fun getEmptyMessageForTab(position: Int): String? =
        when (position) {
            TEMPLATES_TAB_INDEX -> getString(R.string.Uwazi_Templates_Empty_Description)
            DRAFT_LIST_PAGE_INDEX + listTabOffset ->
                getString(R.string.Uwazi_Draft_Entities_Empty_Description)
            OUTBOX_LIST_PAGE_INDEX + listTabOffset ->
                getString(R.string.Uwazi_Outbox_Entities_Empty_Description)
            SUBMITTED_LIST_PAGE_INDEX + listTabOffset ->
                getString(R.string.Uwazi_Submitted_Entities_Empty_Description)
            else -> null
        }
}
