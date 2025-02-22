package org.horizontal.tella.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.event.CamouflageAliasChangedEvent
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.databinding.OnboardCalculatorFragmentBinding
import org.horizontal.tella.mobile.util.CamouflageManager
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class OnBoardCalculatorFragment :
    BaseBindingFragment<OnboardCalculatorFragmentBinding>(OnboardCalculatorFragmentBinding::inflate) {

    private val cm by lazy { CamouflageManager.getInstance() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    fun initView(view: View) {
        (activity as OnBoardActivityInterface).hideProgress()

        val calcButton = view.findViewById<TextView>(R.id.calculatorBtn)
        calcButton.setOnClickListener {
            confirmHideBehindCalculator()
            (activity as OnBoardingActivity).addFragment(
                this,
                OnBoardHideTellaSet(),
                R.id.rootOnboard
            )
        }

        val backBtn = view.findViewById<TextView>(R.id.back_btn)
        backBtn.setOnClickListener {
            (activity as OnBoardingActivity).onBackPressed()
        }
    }

    private fun confirmHideBehindCalculator() {
        BottomSheetUtils.showConfirmSheetWithImageAndTimeout(
            (activity as OnBoardingActivity).supportFragmentManager,
            getString(R.string.SettingsCamo_Dialog_TimeoutTitle),
            getString(R.string.SettingsCamo_Dialog_TimeoutDesc),
            getString(R.string.settings_sec_confirm_camouflage_title),
            getString(R.string.settings_sec_confirm_calc_camouflage_desc),
            getString(R.string.settings_sec_confirm_exit_tella),
            getString(R.string.action_cancel),
            ContextCompat.getDrawable(
                (activity as OnBoardingActivity),
                cm.getCalculatorOptionByTheme(Preferences.getCalculatorTheme()).drawableResId
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
                (activity as OnBoardingActivity),
                cm.getCalculatorOptionByTheme(Preferences.getCalculatorTheme()).alias
            )
        )
            baseActivity.divviupUtils.runCamouflageEnabledEvent()
        MyApplication.bus()
            .post(CamouflageAliasChangedEvent())
    }

}