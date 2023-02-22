package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.databinding.ActivityOnBoardCalculatorBinding
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

private const val NUM_PAGES = 3

class SettingsCalculatorActivity : BaseActivity() {
    private lateinit var binding: ActivityOnBoardCalculatorBinding
    private val cm = CamouflageManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment =
                HideBehindCalculator.getInstance(position)
    }


    fun initView() {
        binding.calculatorBtn.setOnClickListener {
            confirmHideBehindCalculator()
        }
        binding.toolbar.backClickListener = { this.onBackPressed() }
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
                ContextCompat.getDrawable(this, cm.calculatorOption.drawableResId),
                consumer = object : BottomSheetUtils.ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) {
                        hideTellaBehindCalculator()
                    }
                }
        )
    }

    private fun hideTellaBehindCalculator() {
        if (cm.setLauncherActivityAlias(this, CALCULATOR_ALIAS)) {
            MyApplication.bus()
                    .post(CamouflageAliasChangedEvent())
        }
    }
}