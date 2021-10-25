package rs.readahead.washington.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hzontal.tella_locking_ui.CALCULATOR_ALIAS
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class OnBoardCalculatorFragment : BaseFragment() {

    private val cm = CamouflageManager.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_calculator_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnBoardActivityInterface).hideProgress()

        val calcButton = view.findViewById<TextView>(R.id.calculatorBtn)
        calcButton.setOnClickListener {
            confirmHideBehindCalculator()
            activity.addFragment(
                this,
                OnBoardHideTellaSet(),
                R.id.rootOnboard
            )
        }

        val backBtn = view.findViewById<TextView>(R.id.back_btn)
        backBtn.setOnClickListener {
            activity.onBackPressed()
        }
    }

    private fun confirmHideBehindCalculator() {
        BottomSheetUtils.showConfirmSheetWithImage(
            activity.supportFragmentManager,
            getString(R.string.settings_sec_confirm_camouflage_title),
            getString(R.string.settings_sec_confirm_calc_camouflage_desc),
            getString(R.string.settings_sec_confirm_exit_tella),
            getString(R.string.action_cancel),
            ContextCompat.getDrawable(activity,cm.calculatorOption.drawableResId),
            consumer = object : BottomSheetUtils.ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    hideTellaBehindCalculator()
                }
            }
        )
    }

    private fun hideTellaBehindCalculator() {
        if (cm.setLauncherActivityAlias(requireContext(), CALCULATOR_ALIAS)) {
            MyApplication.bus()
                .post(CamouflageAliasChangedEvent())
        }
    }
}