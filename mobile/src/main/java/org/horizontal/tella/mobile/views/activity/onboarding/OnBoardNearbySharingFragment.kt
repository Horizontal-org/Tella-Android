package org.horizontal.tella.mobile.views.activity.onboarding

hhhimport android.os.Bundle
import android.view.View
import org.horizontal.tella.mobile.databinding.OnboardNearbySharingFragmentBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class OnBoardNearbySharingFragment :
    BaseBindingFragment<OnboardNearbySharingFragmentBinding>(OnboardNearbySharingFragmentBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
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

    private fun initView() {
        binding.backBtn.setOnClickListener {
            baseActivity.onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            (baseActivity as OnBoardingActivity).onNextPressed()
        }
    }
}