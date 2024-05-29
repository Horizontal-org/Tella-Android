package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayoutMediator
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentUwaziBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.DRAFT_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.OUTBOX_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.TEMPLATES_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.ViewPagerAdapter

class UwaziFragment : BaseBindingFragment<FragmentUwaziBinding>(FragmentUwaziBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() {
        val viewPagerAdapter = ViewPagerAdapter(this)
        with(binding) {
            viewPager.apply {
                offscreenPageLimit = 4
                isSaveEnabled = false
                adapter = viewPagerAdapter
            }
            // Set the text for each tab
            TabLayoutMediator(tabs, viewPager) { tab, position ->
                tab.text = getTabTitle(position)

            }.attach()

            tabs.setTabTextColors(
                ContextCompat.getColor(baseActivity, R.color.wa_white_50),
                ContextCompat.getColor(baseActivity, R.color.wa_white)
            )

            fabButton.setOnClickListener {
                navManager().navigateFromUwaziScreenToDownloadScreen()
            }
        }

        SharedLiveData.updateViewPagerPosition.observe(baseActivity) { position ->
            when (position) {
                TEMPLATES_LIST_PAGE_INDEX -> setCurrentTab(TEMPLATES_LIST_PAGE_INDEX)
                DRAFT_LIST_PAGE_INDEX -> setCurrentTab(DRAFT_LIST_PAGE_INDEX)
                OUTBOX_LIST_PAGE_INDEX -> setCurrentTab(OUTBOX_LIST_PAGE_INDEX)
                SUBMITTED_LIST_PAGE_INDEX -> setCurrentTab(SUBMITTED_LIST_PAGE_INDEX)
            }
        }

        binding.toolbar.backClickListener = { nav().popBackStack() }
    }

    private fun setCurrentTab(position: Int) {
        if (isViewInitialized) {
            binding.viewPager.post {
                binding.viewPager.setCurrentItem(position, true)
            }
        }
    }

    private fun getTabTitle(position: Int): String? {
        return when (position) {
            TEMPLATES_LIST_PAGE_INDEX -> getString(R.string.Uwazi_Templates_TabTitle)
            DRAFT_LIST_PAGE_INDEX -> getString(R.string.collect_draft_tab_title)
            OUTBOX_LIST_PAGE_INDEX -> getString(R.string.collect_outbox_tab_title)
            SUBMITTED_LIST_PAGE_INDEX -> getString(R.string.collect_sent_tab_title)
            else -> null
        }
    }

}