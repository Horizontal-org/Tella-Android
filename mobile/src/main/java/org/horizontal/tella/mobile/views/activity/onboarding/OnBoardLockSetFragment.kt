package org.horizontal.tella.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.OnboardLockSetFragmentBinding
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.views.base_ui.BaseFragment

class OnBoardLockSetFragment : BaseFragment() {

    private lateinit var binding: OnboardLockSetFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = OnboardLockSetFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        (baseActivity as OnBoardActivityInterface).setCurrentIndicator(3)
        (baseActivity as OnBoardingActivity).hideViewpager()
        (baseActivity as OnBoardingActivity).showProgress()
        with(binding) {
            nextBtn.setOnClickListener {
                baseActivity.addFragment(
                    this@OnBoardLockSetFragment,
                    OnBoardAllDoneFragment(),
                    R.id.rootOnboard
                )
            }
            backBtn.hide()
        }
    }
}