package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

class OnBoardingActivity : BaseActivity() {
    private val isFromSettings by lazy { intent.getBooleanExtra(IS_FROM_SETTINGS,false)  }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboard_container)

        replaceFragmentNoAddToBackStack(if (!isFromSettings) OnBoardIntroFragment() else OnBoardLockFragment.newInstance(true), R.id.rootOnboard)
    }

}