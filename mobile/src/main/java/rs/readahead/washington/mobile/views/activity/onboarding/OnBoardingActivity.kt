package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

class OnBoardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboard_container)
        replaceFragmentNoAddToBackStack(OnBoardIntroFragment(), R.id.rootOnboard)
    }

}