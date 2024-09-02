package rs.readahead.washington.mobile.views.fragment.main_connexions.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.MainReportConnexionBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel

class MainReportFragment:
    BaseBindingFragment<MainReportConnexionBinding>(MainReportConnexionBinding::inflate) {
    private val viewModel by viewModels<ReportsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() {
        val reportsFragmentProvider = ReportsFragmentProvider()
        binding.viewPagerComponent.setupTabs(reportsFragmentProvider,3)
        binding.viewPagerComponent.setTabTitles(listOf(getString(R.string.collect_draft_tab_title), getString(R.string.collect_outbox_tab_title), getString(R.string.collect_sent_tab_title)))
    }

}