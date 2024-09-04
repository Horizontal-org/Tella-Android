package rs.readahead.washington.mobile.views.fragment.main_connexions.fragment

import android.os.Bundle
import android.view.View
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.MainReportConnexionBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment

abstract class MainReportFragment :
    BaseBindingFragment<MainReportConnexionBinding>(MainReportConnexionBinding::inflate) {

    // Abstract method to be implemented by subclasses to provide their own FragmentProvider
    abstract fun getFragmentProvider(): FragmentProvider

    // Abstract method to be implemented by subclasses to provide their own toolbar title
    abstract fun getToolbarTitle(): String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        // Setup the view with the fragment provider from the subclass
        val fragmentProvider = getFragmentProvider()
        binding.viewPagerComponent.setupTabs(fragmentProvider, 3)
        binding.viewPagerComponent.setTabTitles(
            listOf(
                getString(R.string.collect_draft_tab_title),
                getString(R.string.collect_outbox_tab_title),
                getString(R.string.collect_sent_tab_title)
            )
        )
        binding.viewPagerComponent.setToolBarTitle(getToolbarTitle())

        binding.newReportBtn.setOnClickListener {
            this.navManager().navigateFromReportsScreenToNewReportScreen()
        }
    }
}