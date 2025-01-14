package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.MainReportConnexionBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateOutboxTitle
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateSubmittedTitle
import org.horizontal.tella.mobile.views.fragment.vault.attachements.OnNavBckListener


abstract class MainReportFragment :
    BaseBindingFragment<MainReportConnexionBinding>(MainReportConnexionBinding::inflate),
    OnNavBckListener, EmptyMessageVisibilityHandler {

    protected abstract val viewModel: BaseReportsViewModel

    // Abstract method to be implemented by subclasses to provide their own FragmentProvider
    abstract fun getFragmentProvider(): FragmentProvider

    // Abstract method to be implemented by subclasses to provide their own toolbar title
    abstract fun getToolbarTitle(): String
    abstract fun navigateToNewReportScreen()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    protected abstract fun getEmptyMessageIcon(): Int

    private fun initView() {
        if (isViewInitialized) {
            // Setup the view with the fragment provider from the subclass
            val fragmentProvider = getFragmentProvider()
            binding.viewPagerComponent.setTabTitles(
                listOf(
                    getString(R.string.collect_draft_tab_title),
                    getString(R.string.collect_outbox_tab_title),
                    getString(R.string.collect_sent_tab_title)
                )
            )
            viewModel.listOutboxAndSubmitted()

            binding.viewPagerComponent.initViewPager(childFragmentManager, lifecycle, 3)
            binding.viewPagerComponent.setupTabs(fragmentProvider, 3)

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

            updateOutboxTitle.observe(viewLifecycleOwner) { outBoxesSize ->
                binding.viewPagerComponent.updateTabTitle(
                    OUTBOX_LIST_PAGE_INDEX, outBoxesSize
                )
            }

            updateSubmittedTitle.observe(viewLifecycleOwner) { outBoxesSize ->
                binding.viewPagerComponent.updateTabTitle(
                    SUBMITTED_LIST_PAGE_INDEX, outBoxesSize
                )
            }

            viewModel.reportCounts.observe(viewLifecycleOwner) { reportCounts ->
                binding.viewPagerComponent.updateTabTitle(
                    OUTBOX_LIST_PAGE_INDEX, reportCounts.outboxCount
                )

                binding.viewPagerComponent.updateTabTitle(
                    SUBMITTED_LIST_PAGE_INDEX, reportCounts.submittedCount
                )
            }

        }

        setUpEmptyTextViewMessage()
    }

    private fun setCurrentTab(position: Int) {
        if (isViewInitialized) {
            binding.viewPagerComponent.getViewPager().post {
                binding.viewPagerComponent.getViewPager().setCurrentItem(position, true)
            }
        }
    }

    private fun setUpEmptyTextViewMessage() {
        binding.viewPagerComponent.getViewPager()
            .registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    setUpEmptyTextViewMessage(position)
                }
            })
    }

    private fun setUpEmptyTextViewMessage(position: Int) {
        when (position) {
            DRAFT_LIST_PAGE_INDEX -> binding.viewPagerComponent.setCenterMessageImg(
                getString(R.string.Drafts_Reports_Empty_Message), getEmptyMessageIcon()
            )

            OUTBOX_LIST_PAGE_INDEX -> binding.viewPagerComponent.setCenterMessageImg(
                getString(R.string.Outbox_Reports_Empty_Message), getEmptyMessageIcon()
            )

            SUBMITTED_LIST_PAGE_INDEX -> binding.viewPagerComponent.setCenterMessageImg(
                getString(R.string.Submitted_Reports_Empty_Message), getEmptyMessageIcon()
            )
        }
    }


    override fun onBackPressed(): Boolean {
        back()
        return true
    }

    override fun setEmptyTextViewMessageVisibility(isVisible: Boolean) {
        binding.viewPagerComponent.setEmptyTextViewMessageVisibility(isVisible)
    }
}