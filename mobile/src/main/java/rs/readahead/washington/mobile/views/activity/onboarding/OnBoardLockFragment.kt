package rs.readahead.washington.mobile.views.activity.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.ui.password.SetPasswordActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pin.SetPinActivity
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.custom.InformationButton

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

    private fun initView(view: View) {
       arguments?.let { isFromSettings=  it.getBoolean(IS_FROM_SETTINGS,false) }
        lockPasswordBtn = view.findViewById(R.id.lockPasswordBtn)
        lockPINdBtn = view.findViewById(R.id.lockPINdBtn)
        lockPatternBtn = view.findViewById(R.id.lockPatternBtn)
        cancelBtn = view.findViewById(R.id.cancelBtn)
        if (isFromSettings) cancelBtn.visibility = View.VISIBLE
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

        cancelBtn.setOnClickListener { activity.finish() }
    }

    private fun toggleButtons(passwordState: Boolean, pinState: Boolean, patternState: Boolean) {
        lockPasswordBtn.isChecked = passwordState
        lockPINdBtn.isChecked = pinState
        lockPatternBtn.isChecked = patternState
    }

    private fun goUnlockingActivity(destination : Activity){
        startActivity(Intent(activity, destination::class.java))
        if (isFromSettings) activity.finish()
    }

}