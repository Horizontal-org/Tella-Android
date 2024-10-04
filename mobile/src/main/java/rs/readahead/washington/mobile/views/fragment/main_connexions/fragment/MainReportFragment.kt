package rs.readahead.washington.mobile.views.fragment.main_connexions.fragment

import android.os.Bundle
import android.view.View
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.MainReportConnexionBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.DRAFT_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.OUTBOX_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.SharedLiveData


abstract class MainReportFragment :
    BaseBindingFragment<MainReportConnexionBinding>(MainReportConnexionBinding::inflate) {

    // Abstract method to be implemented by subclasses to provide their own FragmentProvider
    abstract fun getFragmentProvider(): FragmentProvider

    // Abstract method to be implemented by subclasses to provide their own toolbar title
    abstract fun getToolbarTitle(): String
    abstract fun navigateToNewReportScreen()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        // Setup the view with the fragment provider from the subclass
        val fragmentProvider = getFragmentProvider()
        binding.viewPagerComponent.initViewPager(childFragmentManager, lifecycle, 3)
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
            navigateToNewReportScreen()
        }
        binding.viewPagerComponent.setOnToolbarBackClickListener { back() }

        SharedLiveData.updateViewPagerPosition.observe(baseActivity) { position ->
            when (position) {
                DRAFT_LIST_PAGE_INDEX -> setCurrentTab(DRAFT_LIST_PAGE_INDEX)
                OUTBOX_LIST_PAGE_INDEX -> setCurrentTab(OUTBOX_LIST_PAGE_INDEX)
                SUBMITTED_LIST_PAGE_INDEX -> setCurrentTab(SUBMITTED_LIST_PAGE_INDEX)
            }
        }
    }

    private fun setCurrentTab(position: Int) {
        if (isViewInitialized) {
            binding.viewPagerComponent.getViewPager().post {
                binding.viewPagerComponent.getViewPager().setCurrentItem(position, true)
            }
        }
    }
}