package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.MainReportConnexionBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateDraftTitle
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateOutboxTitle
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData.updateSubmittedTitle
import org.horizontal.tella.mobile.views.fragment.vault.attachements.OnNavBckListener


abstract class MainReportFragment :
    BaseBindingFragment<MainReportConnexionBinding>(MainReportConnexionBinding::inflate),
    OnNavBckListener, EmptyMessageVisibilityHandler {

    protected abstract val viewModel: BaseEntityListViewModel<*>

    // Abstract method to be implemented by subclasses to provide their own FragmentProvider
    abstract fun getFragmentProvider(): FragmentProvider

    // Abstract method to be implemented by subclasses to provide their own toolbar title
    abstract fun getToolbarTitle(): String
    abstract fun navigateToNewReportScreen()

    /**
     * Tabs preceding draft/outbox/submitted. Uwazi shows a Templates tab first, so its
     * draft/outbox/submitted pages sit one position further right than the other connections'.
     */
    protected open val listTabOffset: Int = 0

    protected open fun getTabTitles(): List<String> = listOf(
        getString(R.string.collect_draft_tab_title),
        getString(R.string.collect_outbox_tab_title),
        getString(R.string.collect_sent_tab_title)
    )

    protected open fun getNewButtonText(): Int = R.string.New_Reports_Text

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    protected abstract fun getEmptyMessageIcon(): Int

    private fun initView() {
        if (isViewInitialized) {
            // Setup the view with the fragment provider from the subclass
            val fragmentProvider = getFragmentProvider()
            val tabTitles = getTabTitles()
            val tabCount = tabTitles.size
            binding.viewPagerComponent.setTabTitles(tabTitles)
            viewModel.listDraftsOutboxAndSubmitted()

            binding.viewPagerComponent.initViewPager(childFragmentManager, lifecycle, tabCount)
            binding.viewPagerComponent.setupTabs(fragmentProvider, tabCount)

            binding.viewPagerComponent.setToolBarTitle(getToolbarTitle())

            binding.newReportBtn.setText(getNewButtonText())
            binding.newReportBtn.setOnClickListener {
                navigateToNewReportScreen()
            }
            binding.viewPagerComponent.setOnToolbarBackClickListener { back() }

            SharedLiveData.updateViewPagerPosition.observe(baseActivity) { position ->
                when (position) {
                    DRAFT_LIST_PAGE_INDEX,
                    OUTBOX_LIST_PAGE_INDEX,
                    SUBMITTED_LIST_PAGE_INDEX -> setCurrentTab(position + listTabOffset)
                }
            }

            updateOutboxTitle.observe(viewLifecycleOwner) { outBoxesSize ->
                updateTabTitle(OUTBOX_LIST_PAGE_INDEX, outBoxesSize)
            }

            updateSubmittedTitle.observe(viewLifecycleOwner) { outBoxesSize ->
                updateTabTitle(SUBMITTED_LIST_PAGE_INDEX, outBoxesSize)
            }

            updateDraftTitle.observe(viewLifecycleOwner) { outBoxesSize ->
                updateTabTitle(DRAFT_LIST_PAGE_INDEX, outBoxesSize)
            }


            viewModel.reportCounts.observe(viewLifecycleOwner) { reportCounts ->
                updateTabTitle(DRAFT_LIST_PAGE_INDEX, reportCounts.draftCounts)
                updateTabTitle(OUTBOX_LIST_PAGE_INDEX, reportCounts.outboxCount)
                updateTabTitle(SUBMITTED_LIST_PAGE_INDEX, reportCounts.submittedCount)
            }

        }

        setUpEmptyTextViewMessage()
    }

    protected fun updateTabTitle(listPageIndex: Int, count: Int) {
        binding.viewPagerComponent.updateTabTitle(listPageIndex + listTabOffset, count)
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
        val message = getEmptyMessageForTab(position) ?: return
        binding.viewPagerComponent.setCenterMessageImg(message, getEmptyMessageIcon())
    }

    protected open fun getEmptyMessageForTab(position: Int): String? =
        when (position - listTabOffset) {
            DRAFT_LIST_PAGE_INDEX -> getString(R.string.Drafts_Reports_Empty_Message)
            OUTBOX_LIST_PAGE_INDEX -> getString(R.string.Outbox_Reports_Empty_Message)
            SUBMITTED_LIST_PAGE_INDEX -> getString(R.string.Submitted_Reports_Empty_Message)
            else -> null
        }

    override fun onBackPressed(): Boolean {
        back()
        return true
    }

    override fun setEmptyTextViewMessageVisibility(isVisible: Boolean) {
        binding.viewPagerComponent.setEmptyTextViewMessageVisibility(isVisible)
    }
}
