package rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments

import android.os.Bundle
import android.view.View
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentReportsListBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment

class SubmittedReportsFragment  : BaseBindingFragment<FragmentReportsListBinding>(
    FragmentReportsListBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView(){
        binding?.textViewEmpty?.setText(getString(R.string.Submitted_Reports_Empty_Message))
    }

}