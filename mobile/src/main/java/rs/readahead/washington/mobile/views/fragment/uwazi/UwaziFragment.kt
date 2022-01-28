package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentUwaziBinding
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.viewpager.*

class UwaziFragment : BaseFragment() {

    private  var binding: FragmentUwaziBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUwaziBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun initView(view: View) {
        val viewPagerAdapter  = ViewPagerAdapter(this)
        with(binding!!){
            viewPager.apply {
                adapter = viewPagerAdapter
            }
            // Set the text for each tab
            TabLayoutMediator(tabs, viewPager) { tab, position ->
                tab.text = getTabTitle(position)
            }.attach()

            fabButton.setOnClickListener {
                nav().navigate(R.id.action_uwaziScreen_to_uwaziDownloadScreen)
            }
        }

    }

    private fun getTabTitle(position: Int): String? {
        return when (position) {
            TEMPLATES_LIST_PAGE_INDEX -> getString(R.string.uwazi_outbox_tab_title)
            DRAFT_LIST_PAGE_INDEX -> getString(R.string.collect_draft_tab_title)
            OUTBOX_LIST_PAGE_INDEX -> getString(R.string.collect_outbox_tab_title)
            SUBMITTED_LIST_PAGE_INDEX -> getString(R.string.collect_sent_tab_title)
            else -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewPager?.adapter = null
        binding = null
    }

}