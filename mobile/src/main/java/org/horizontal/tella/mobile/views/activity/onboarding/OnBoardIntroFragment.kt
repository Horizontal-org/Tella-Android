package org.horizontal.tella.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.base_ui.BaseFragment

class OnBoardIntroFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_intro_fragment_1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        val enterCodeButton = view.findViewById<TextView>(R.id.sheet_two_btn)
        enterCodeButton.setOnClickListener {
            (baseActivity as OnBoardActivityInterface).enterCustomizationCode()
        }

        view.findViewById<TextView>(R.id.startBtn).setOnClickListener {
            (baseActivity as OnBoardingActivity).onNextPressed()
        }
    }
}
