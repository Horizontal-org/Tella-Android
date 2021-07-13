package rs.readahead.washington.mobile.views.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.LockTimeoutManager


class SecuritySettings : Fragment() {
    var lockSetting: TextView? = null
    var lockTimeoutSetting: View? = null
    private val lockTimeoutManager = LockTimeoutManager()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_security_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_sec_app_bar)

        lockSetting = view.findViewById(R.id.lock_setting)
        lockTimeoutSetting = view.findViewById(R.id.lock_timeout_setting)

        val lockSettingButton = view.findViewById<RelativeLayout>(R.id.lock_settings_button)
        lockSettingButton.setOnClickListener { goToUnlockingActivity() }

        val lockTimeoutSettingButton = view.findViewById<RelativeLayout>(R.id.lock_timeout_settings_button)
        lockTimeoutSettingButton.setOnClickListener { showLockTimeoutSettingDialog() }

        return view
    }

    private fun showLockTimeoutSettingDialog() {
        val optionConsumer = object : BottomSheetUtils.LockOptionConsumer {
            override fun accept(option: Long) {
                onLockTimeoutChoosen(option)
            }
        }
        activity?.let {
            BottomSheetUtils.showRadioListSheet(requireActivity().supportFragmentManager,
                    requireContext(),
                    lockTimeoutManager.lockTimeout,
                    lockTimeoutManager.getOptionsList(),
                    getString(R.string.settings_select_lock_timeout),
                    getString(R.string.settings_sec_lock_timeout_desc),
                    getString(R.string.action_ok),
                    getString(R.string.action_cancel),
                    optionConsumer)
        }
    }

    private fun onLockTimeoutChoosen(option: Long) {
        lockTimeoutManager.setLockTimeout(option)
    }

    private fun setUpLockTypeText() {
        when ((activity?.getApplicationContext() as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(activity)) {
            UnlockRegistry.Method.TELLA_PIN -> lockSetting?.setText(getString(R.string.onboard_pin))
            UnlockRegistry.Method.TELLA_PASSWORD -> lockSetting?.setText(getString(R.string.onboard_password))
            UnlockRegistry.Method.TELLA_PATTERN -> lockSetting?.setText(getString(R.string.onboard_pattern))
        }
    }

    fun goToUnlockingActivity() {
        var intent: Intent? = null
        when ((activity?.getApplicationContext() as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(activity)) {
            UnlockRegistry.Method.TELLA_PIN -> intent = Intent(activity, PinUnlockActivity::class.java)
            UnlockRegistry.Method.TELLA_PASSWORD -> intent = Intent(activity, PasswordUnlockActivity::class.java)
            UnlockRegistry.Method.TELLA_PATTERN -> intent = Intent(activity, PatternUnlockActivity::class.java)
        }
        intent!!.putExtra(IS_FROM_SETTINGS, true)
        startActivity(intent)
    }
}