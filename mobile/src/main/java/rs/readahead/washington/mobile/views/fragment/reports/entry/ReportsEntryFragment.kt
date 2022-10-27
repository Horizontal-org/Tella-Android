package rs.readahead.washington.mobile.views.fragment.reports.entry

import android.os.Bundle
import android.view.View
import rs.readahead.washington.mobile.databinding.FragmentReportsEntryBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment


class ReportsEntryFragment:
    BaseBindingFragment<FragmentReportsEntryBinding>(FragmentReportsEntryBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() {
    }
}