package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.CalculatorTheme
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.ActivityOnBoardCalculatorBinding
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

private const val NUM_PAGES = 4
private const val CALCULATOR_GREEN_SKIN_INDEX = 0
private const val CALCULATOR_ORANGE_SKIN_INDEX = 1
private const val CALCULATOR_BLUE_SKIN_INDEX = 2
private const val CALCULATOR_YELLOW_SKIN_INDEX = 3

class SettingsCalculatorActivity : BaseActivity() {
    private lateinit var binding: ActivityOnBoardCalculatorBinding
    private val cm = CamouflageManager.getInstance()
    private lateinit var calculatorTheme: CalculatorTheme
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
    }

    fun initView() {
        binding.calculatorBtn.setOnClickListener {
            setTheme()
            confirmHideBehindCalculator()
        }
        binding.toolbar.backClickListener = { this.onBackPressed() }
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        binding.nextBtn.setOnClickListener {
            onNextPressed()
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    CALCULATOR_GREEN_SKIN_INDEX -> showButtons(
                        isNextButtonVisible = true,
                        isBackButtonVisible = false
                    )

                    CALCULATOR_ORANGE_SKIN_INDEX -> showButtons(
                        isNextButtonVisible = true,
                        isBackButtonVisible = true
                    )

                    CALCULATOR_BLUE_SKIN_INDEX -> showButtons(
                        isNextButtonVisible = true,
                        isBackButtonVisible = true
                    )

                    CALCULATOR_YELLOW_SKIN_INDEX -> showButtons(
                        isNextButtonVisible = false,
                        isBackButtonVisible = true
                    )
                }
                super.onPageSelected(position)
            }
        })
    }


    private fun confirmHideBehindCalculator() {
        BottomSheetUtils.showConfirmSheetWithImageAndTimeout(
            this.supportFragmentManager,
            getString(R.string.SettingsCamo_Dialog_TimeoutTitle),
            getString(R.string.SettingsCamo_Dialog_TimeoutDesc),
            getString(R.string.settings_sec_confirm_camouflage_title),
            getString(R.string.settings_sec_confirm_calc_camouflage_desc),
            getString(R.string.settings_sec_confirm_exit_tella),
            getString(R.string.action_cancel),
            ContextCompat.getDrawable(
                this,
                cm.getCalculatorOptionByTheme(calculatorTheme.name).drawableResId
            ),
            consumer = object : BottomSheetUtils.ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    hideTellaBehindCalculator()
                }
            }
        )
    }

    private fun hideTellaBehindCalculator() {
        if (cm.setLauncherActivityAlias(
                this,
                cm.getCalculatorOptionByTheme(calculatorTheme.name).alias
            )
        )
            divviupUtils.runCamouflageEnabledEvent()
        MyApplication.bus()
            .post(CamouflageAliasChangedEvent())
    }


    private fun setTheme() {
        when (binding.viewPager.currentItem) {
            CALCULATOR_GREEN_SKIN_INDEX -> {
                calculatorTheme = CalculatorTheme.GREEN_SKIN
            }

            CALCULATOR_ORANGE_SKIN_INDEX -> {
                calculatorTheme = CalculatorTheme.ORANGE_SKIN
            }

            CALCULATOR_BLUE_SKIN_INDEX -> {
                calculatorTheme = CalculatorTheme.BLUE_SKIN
            }

            CALCULATOR_YELLOW_SKIN_INDEX -> {
                calculatorTheme = CalculatorTheme.YELLOW_SKIN
            }

        }
        Preferences.setCalculatorTheme(calculatorTheme.name)
    }


    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            if (binding.viewPager.size > 0)
                binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }

    }

    private fun onNextPressed() {
        if (binding.viewPager.currentItem != NUM_PAGES) {
            // select the Next step in the viewpager
            binding.viewPager.currentItem = binding.viewPager.currentItem + 1
        }
    }

    fun showButtons(isNextButtonVisible: Boolean, isBackButtonVisible: Boolean) {
        binding.nextBtn.isVisible = isNextButtonVisible
        binding.backBtn.isVisible = isBackButtonVisible
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment {
            return HideBehindCalculator.getInstance(position)

        }
    }
}