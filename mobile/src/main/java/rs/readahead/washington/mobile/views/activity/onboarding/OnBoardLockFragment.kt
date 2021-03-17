package rs.readahead.washington.mobile.views.activity.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hzontal.tella_locking_ui.ui.password.SetPasswordActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pin.SetPinActivity
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.custom.InformationButton

class OnBoardLockFragment : BaseFragment(){
    private lateinit var lockPasswordBtn : InformationButton
    private lateinit var lockPINdBtn : InformationButton
    private lateinit var lockPatternBtn : InformationButton
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

    private fun initView(view: View){
        lockPasswordBtn = view.findViewById(R.id.lockPasswordBtn)
        lockPINdBtn = view.findViewById(R.id.lockPINdBtn)
        lockPatternBtn = view.findViewById(R.id.lockPatternBtn)

        initListeners()
    }

    private fun initListeners(){
        lockPasswordBtn.setOnClickListener{
            toggleButtons(passwordState = true, pinState = false, patternState = false)
            startActivity(Intent(activity, SetPasswordActivity::class.java))}
        lockPINdBtn.setOnClickListener{
            toggleButtons(passwordState = false, pinState = true, patternState = false)
            startActivity(Intent(activity, SetPinActivity::class.java))}
        lockPatternBtn.setOnClickListener{
            toggleButtons(passwordState = false, pinState = false, patternState = true)
            startActivity(Intent(activity, PatternSetActivity::class.java))}
    }

    private fun toggleButtons(passwordState : Boolean,pinState : Boolean, patternState: Boolean){
        lockPasswordBtn.isChecked = passwordState
        lockPINdBtn.isChecked  = pinState
        lockPatternBtn.isChecked = patternState
    }


}