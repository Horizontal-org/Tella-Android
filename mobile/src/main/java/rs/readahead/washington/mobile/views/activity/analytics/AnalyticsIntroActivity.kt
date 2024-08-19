package rs.readahead.washington.mobile.views.activity.analytics

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

class AnalyticsIntroActivity : BaseActivity() {

    companion object {
        const val RESULT_FOR_ACTIVITY = "result_from_activity"
        const val CLEAN_INSIGHTS_REQUEST_CODE = 47551
    }

    private var viewPagerPosition = 0
    private val fragmentAdapter by lazy {
        ViewPagerFragmentAdapter(
            this, arrayListOf(
                AnalyticsShareDataFragment.newInstance { onNextPage() },
                AnalyticsHowWorksFragment.newInstance({ onNextPage() }, { onBack() }),
                AnalyticsWeWillNotFragment.newInstance({ onNextPage() }, { onBack() }),
                AnalyticsQuestionsFragment.newInstance({ onNextPage() }, { onBack() }),
                AnalyticsContributeFragment.newInstance({ optedIn -> onNextPage(optedIn) }, { onBack() })
            )
        )
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(com.hzontal.tella_locking_ui.R.anim.`in`, com.hzontal.tella_locking_ui.R.anim.out)
        setContentView(R.layout.activity_clean_insghts)
        findViewById<ImageView>(R.id.img_close).setOnClickListener { returnToActivityWithResult(AnalyticsActions.CLOSE) }
        indicatorsContainer = findViewById(R.id.indicatorsContainer)
        viewPager = findViewById(R.id.viewpager_clean_insights)
        setupIndicators()
        initViewPager()
    }

    private fun onNextPage(optedIn: AnalyticsActions = AnalyticsActions.CLOSE) {
        when (viewPagerPosition) {
            4 -> returnToActivityWithResult(optedIn)
            else -> viewPager.currentItem = viewPagerPosition.inc()
        }
    }

    private fun onBack() {
        viewPager.currentItem = viewPagerPosition.dec()
    }

    private fun initViewPager() {
        with(viewPager) {
            adapter = fragmentAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            isUserInputEnabled = true
            offscreenPageLimit = 2
            onPageChange { viewPagerPosition = it }
        }
    }

    private fun ViewPager2.onPageChange(onChange: (Int) -> Unit) {
        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                onChange(position)
            }
        })
    }

    private fun setupIndicators() {
        indicatorsContainer.removeAllViews()
        val indicators = arrayOfNulls<ImageView>(5)
        val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(12, 0, 12, 0)
        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext)
            indicators[i].apply {
                this?.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.onboarding_indicator_inactive))
                this?.layoutParams = layoutParams
            }
            indicatorsContainer.addView(indicators[i])
        }
    }

    fun setCurrentIndicator(index: Int) {
        val childCount = indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorsContainer[i] as ImageView
            if (i == index) imageView.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.onboarding_indicator_active))
            else imageView.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.onboarding_indicator_inactive))
        }
    }

    private fun returnToActivityWithResult(action: AnalyticsActions) {
        Intent().apply {
            putExtra(RESULT_FOR_ACTIVITY, action)
        }.also {
            setResult(CLEAN_INSIGHTS_REQUEST_CODE, it)
            finish()
        }
    }
}