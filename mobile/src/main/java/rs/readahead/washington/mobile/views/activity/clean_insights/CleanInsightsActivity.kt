package rs.readahead.washington.mobile.views.activity.clean_insights

import android.content.Intent
import android.os.Bundle
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

class CleanInsightsActivity : BaseActivity() {

    companion object {
        const val RESULT_FOR_ACTIVITY = "result_from_activity"
        const val FROM_LEARN_MORE = "is_from_learn_more"
        const val CLEAN_INSIGHTS_REQUEST_CODE = 47551
    }

    private val isFromLearnMore by lazy { intent?.extras?.getBoolean(FROM_LEARN_MORE) ?: false }

    private var viewPagerPosition = 0
    private val fragmentAdapter by lazy {
        ViewPagerFragmentAdapter(
            this, arrayListOf(
                CleanInsightShareDataFragment.newInstance { onNextPage() },
                CleanInsightDaysFragment.newInstance({ onNextPage() }, { onBackPressed() }),
                CleanInsightHowWorksFragment.newInstance({ onNextPage() }, { onBackPressed() }),
                CleanInsightContributeFragment.newInstance(
                    { optedIn -> onNextPage(optedIn) },
                    { onBackPressed() },
                    { returnToActivityWithResult(isOptedIn = false, openLeanMore = true) }
                )
            )
        )
    }

    private val fragmentAdapterLearnMore by lazy {
        ViewPagerFragmentAdapter(
            this, arrayListOf(
                CleanInsightQuestionsFragment.newInstance { onNextPage() },
                CleanInsightWeWillFragment.newInstance({ onNextPage() }, { onBackPressed() }),
                CleanInsightWeWillNotFragment.newInstance({ onNextPage() }, { onBackPressed() }),
                CleanInsightAggregatedFragment.newInstance({ onNextPage() }, { onBackPressed() }),
                CleanInsightContributeFragment.newInstance(
                    { optedIn -> onNextPage(optedIn) }, { onBackPressed() }, { }, true
                )
            )
        )
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(
            com.hzontal.tella_locking_ui.R.anim.`in`,
            com.hzontal.tella_locking_ui.R.anim.out
        )
        setContentView(R.layout.activity_clean_insghts)
        if (isFromLearnMore) findViewById<TextView>(R.id.tv_how_works).visibility = VISIBLE
        findViewById<ImageView>(R.id.img_close).setOnClickListener { finish() }
        indicatorsContainer = findViewById(R.id.indicatorsContainer)
        viewPager = findViewById(R.id.viewpager_clean_insights)
        setupIndicators()
        initViewPager()
    }

    private fun onNextPage(optedIn: Boolean = false) {
        if (isFromLearnMore) {
            when (viewPagerPosition) {
                4 -> returnToActivityWithResult(optedIn)
                else -> viewPager.currentItem = viewPagerPosition.inc()
            }
        } else {
            when (viewPagerPosition) {
                3 -> returnToActivityWithResult(optedIn)
                else -> viewPager.currentItem = viewPagerPosition.inc()
            }
        }
    }

    override fun onBackPressed() {
        viewPager.currentItem = viewPagerPosition.dec()
    }

    private fun initViewPager() {
        with(viewPager) {
            adapter = if (isFromLearnMore) fragmentAdapterLearnMore else fragmentAdapter
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
        val indicators = arrayOfNulls<ImageView>(if (isFromLearnMore) 5 else 4)
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
    }

    fun setCurrentIndicator(index: Int) {
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
    }

    private fun returnToActivityWithResult(isOptedIn: Boolean, openLeanMore: Boolean = false) {
        Intent().apply {
            putExtra(RESULT_FOR_ACTIVITY, isOptedIn)
            putExtra(FROM_LEARN_MORE, openLeanMore)
        }.also {
            setResult(CLEAN_INSIGHTS_REQUEST_CODE, it)
            finish()
        }
    }
}