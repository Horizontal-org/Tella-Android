package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.IS_ONBOARD_LOCK_SET
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

class OnBoardingActivity : BaseActivity(), OnBoardActivityInterface {
    private val isFromSettings by lazy { intent.getBooleanExtra(IS_FROM_SETTINGS, false)  }
    private val isOnboardLockSet by lazy { intent.getBooleanExtra(IS_ONBOARD_LOCK_SET, false)  }
   // private lateinit var buttonNext: TextView
   // private lateinit var buttonBack: TextView
    private lateinit var indicatorsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(com.hzontal.tella_locking_ui.R.anim.`in`, com.hzontal.tella_locking_ui.R.anim.out)

        //setContentView(R.layout.activity_onboard_container)
        setContentView(R.layout.activity_onboarding)
       // buttonNext = findViewById(R.id.next_btn)
       // buttonBack = findViewById(R.id.back_btn)
        indicatorsContainer = findViewById(R.id.indicatorsContainer)
        //setupIndicators(5)

        if (isOnboardLockSet) {
            replaceFragmentNoAddToBackStack(OnBoardLockSetFragment(), R.id.rootOnboard)
        } else {
            replaceFragmentNoAddToBackStack(
                if (!isFromSettings) OnBoardIntroFragment() else OnBoardLockFragment.newInstance(
                    true
                ), R.id.rootOnboard
            )
        }
    }

    private fun setupIndicators(indicatorCount : Int) {
        indicatorsContainer.removeAllViews()
        val indicators = arrayOfNulls<ImageView>(indicatorCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        layoutParams.setMargins(12, 0, 12, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i].apply {
                this?.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.onboarding_indicator_inactive
                    )
                )
                this?.layoutParams = layoutParams
            }
            indicatorsContainer.addView(indicators[i])
        }

        /*buttonNext.setOnClickListener {
           /* if (onboardingViewpager.currentItem < onboardingSliderAdapter.itemCount) {
                onboardingViewpager.currentItem += 1
            }*/
        }*/

      /*  buttonBack.setOnClickListener {
            onBackPressed()
        //   closeIntro()
        }*/

        /* buttonEnd.setOnClickListener {
             closeIntro()
         }*/
    }

    override fun setCurrentIndicator(index: Int) {
        val childCount = indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorsContainer[i] as ImageView
            if (i == index) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.onboarding_indicator_active
                    )
                )
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.onboarding_indicator_inactive
                    )
                )
            }
        }
        /*if (index + 1 == onboardingSliderAdapter.itemCount) {
            showEndButton()
        } else {
            hideEndButton()
        }*/
    }

    override fun hideProgress() {
        indicatorsContainer.visibility = View.INVISIBLE
    }

    override fun showProgress() {
        indicatorsContainer.visibility = View.VISIBLE
    }

    override fun initProgress(itemCount: Int) {
        setupIndicators(itemCount)
    }
}