package rs.readahead.washington.mobile.views.fragment.reports

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.entry.ReportsEntryViewModel
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.DRAFT_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.OUTBOX_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.ViewPagerAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData

@AndroidEntryPoint
class ReportsFragment :
    BaseBindingFragment<FragmentReportsBinding>(FragmentReportsBinding::inflate) {
    private val viewModel by viewModels<ReportsEntryViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        val viewPagerAdapter = ViewPagerAdapter(this)

        binding?.viewPager?.apply {
            offscreenPageLimit = 3
            isSaveEnabled = false
            adapter = viewPagerAdapter
        }

        // Set the text for each tab
        binding?.viewPager?.let {
            TabLayoutMediator(binding?.tabs!!, it) { tab, position ->
                tab.text = getTabTitle(position)

            }.attach()
        }

        binding?.tabs?.setTabTextColors(
            ContextCompat.getColor(baseActivity, R.color.wa_white_44),
            ContextCompat.getColor(baseActivity, R.color.wa_white)
        )

        binding?.newReportBtn?.setOnClickListener {
            nav().navigate(R.id.action_reportsScreen_to_newReport_screen)
        }
        binding?.toolbar?.backClickListener = { nav().popBackStack() }

    }

    private fun getTabTitle(position: Int): String? {
        return when (position) {
            DRAFT_LIST_PAGE_INDEX -> getString(R.string.collect_draft_tab_title)
            OUTBOX_LIST_PAGE_INDEX -> getString(R.string.collect_outbox_tab_title)
            SUBMITTED_LIST_PAGE_INDEX -> getString(R.string.collect_sent_tab_title)
            else -> null
        }
    }

    private fun initData() {
        SharedLiveData.updateViewPagerPosition.observe(baseActivity, { position ->
            when (position) {
                DRAFT_LIST_PAGE_INDEX -> setCurrentTab(
                    DRAFT_LIST_PAGE_INDEX
                )
                OUTBOX_LIST_PAGE_INDEX -> setCurrentTab(
                    OUTBOX_LIST_PAGE_INDEX
                )
                SUBMITTED_LIST_PAGE_INDEX -> setCurrentTab(
                    SUBMITTED_LIST_PAGE_INDEX
                )
            }
        })
    }

    private fun setCurrentTab(position: Int) {
        binding?.viewPager?.post {
            binding?.viewPager?.setCurrentItem(position, true)
        }
    }
}