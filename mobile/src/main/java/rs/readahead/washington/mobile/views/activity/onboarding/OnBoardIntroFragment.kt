package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

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

    override fun onResume() {
        super.onResume()
        (baseActivity as OnBoardActivityInterface).enableSwipe(
            isSwipeable = false, isTabLayoutVisible = false
        )
        (baseActivity as OnBoardActivityInterface).showButtons(
            isNextButtonVisible = false, isBackButtonVisible = false
        )
    }

    override fun initView(view: View) {
        (baseActivity as OnBoardActivityInterface).hideProgress()

        val enterCodeButton = view.findViewById<TextView>(R.id.sheet_two_btn)
        enterCodeButton.setOnClickListener {
            (baseActivity as OnBoardActivityInterface).enterCustomizationCode()
        }

        val startBtn = view.findViewById<TextView>(R.id.startBtn)
        startBtn.setOnClickListener {
            (baseActivity as OnBoardingActivity).onNextPressed()

        }
    }
}