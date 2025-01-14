package org.horizontal.tella.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.View
import org.horizontal.tella.mobile.databinding.OnboardCollectDataBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

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