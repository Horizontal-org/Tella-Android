package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.OnboardCollectDataBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnboardCollectDataFragment : BaseBindingFragment<OnboardCollectDataBinding>(OnboardCollectDataBinding::inflate) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun onResume() {
        super.onResume()
        (baseActivity as OnBoardActivityInterface).enableSwipe(
            isSwipeable = true, isTabLayoutVisible = true
        )
        (baseActivity as OnBoardActivityInterface).showButtons(
            isNextButtonVisible = true, isBackButtonVisible = true
        )
    }

    private fun initView(view: View) {
        binding.backBtn.setOnClickListener {
            baseActivity.onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            (baseActivity as OnBoardingActivity).onNextPressed()
        }
    }
}