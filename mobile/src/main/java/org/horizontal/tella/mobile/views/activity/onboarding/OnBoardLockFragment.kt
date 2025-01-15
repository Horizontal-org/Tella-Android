package org.horizontal.tella.mobile.views.activity.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.hzontal.shared_ui.buttons.InformationButton
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.ui.password.SetPasswordActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pin.SetPinActivity
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.base_ui.BaseFragment

class OnBoardLockFragment : BaseFragment() {
    private lateinit var lockPasswordBtn: InformationButton
    private lateinit var lockPINdBtn: InformationButton
    private lateinit var lockPatternBtn: InformationButton
    private var isFromSettings = false
    private lateinit var cancelBtn : TextView
    companion object {
        // Use this function to create instance of current fragment
        fun newInstance(isFromSettings : Boolean): OnBoardLockFragment {
            val args = Bundle()
            args.putBoolean(IS_FROM_SETTINGS, isFromSettings)
            val fragment = OnBoardLockFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboard_lock_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun onResume() {
        super.onResume()
        (baseActivity as OnBoardActivityInterface).enableSwipe(
            isSwipeable = true, isTabLayoutVisible = true)
        (baseActivity as OnBoardActivityInterface).showButtons(
            isNextButtonVisible = false, isBackButtonVisible = true)
    }

    override fun initView(view: View) {
        arguments?.let { isFromSettings=  it.getBoolean(IS_FROM_SETTINGS,false) }
        lockPasswordBtn = view.findViewById(R.id.lockPasswordBtn)
        lockPINdBtn = view.findViewById(R.id.lockPINdBtn)
        lockPatternBtn = view.findViewById(R.id.lockPatternBtn)
        cancelBtn = view.findViewById(R.id.cancelBtn)
        if (isFromSettings) {
            cancelBtn.visibility = View.VISIBLE
            (baseActivity as OnBoardingActivity).hideViewpager()

        }
        initListeners()
    }

    private fun initListeners() {

        lockPasswordBtn.setOnClickListener {
            toggleButtons(passwordState = true, pinState = false, patternState = false)
             goUnlockingActivity(SetPasswordActivity())
        }
        lockPINdBtn.setOnClickListener {
            toggleButtons(passwordState = false, pinState = true, patternState = false)
            goUnlockingActivity(SetPinActivity())
        }
        lockPatternBtn.setOnClickListener {
            toggleButtons(passwordState = false, pinState = false, patternState = true)
            goUnlockingActivity(PatternSetActivity())
        }

        cancelBtn.setOnClickListener {
            (baseActivity as OnBoardActivityInterface).setCurrentIndicator(2)
            baseActivity.onBackPressed()
        }
    }

    private fun toggleButtons(passwordState: Boolean, pinState: Boolean, patternState: Boolean) {
        lockPasswordBtn.isChecked = passwordState
        lockPINdBtn.isChecked = pinState
        lockPatternBtn.isChecked = patternState
    }

    private fun goUnlockingActivity(destination : Activity){
        val intent = Intent(baseActivity, destination::class.java)
        intent.putExtra(IS_FROM_SETTINGS,isFromSettings)
        startActivity(intent)
        if (isFromSettings) baseActivity.finish()
    }

}