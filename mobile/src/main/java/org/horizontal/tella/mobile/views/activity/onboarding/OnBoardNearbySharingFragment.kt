package org.horizontal.tella.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.View
import org.horizontal.tella.mobile.databinding.OnboardNearbySharingFragmentBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class OnBoardNearbySharingFragment :
    BaseBindingFragment<OnboardNearbySharingFragmentBinding>(OnboardNearbySharingFragmentBinding::inflate) {

    override fun applyEdgeToEdgeIfNeeded(view: View) {
        OnboardingInsets.applyCarouselSlide(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
