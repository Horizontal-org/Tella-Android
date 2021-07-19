package rs.readahead.washington.mobile.views.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.switches.TellaSwitchWithMessage
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.util.LockTimeoutManager


class SecuritySettings : Fragment() {
    var lockSetting: TextView? = null
    var lockTimeoutSetting: TextView? = null

    private val lockTimeoutManager = LockTimeoutManager()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_security_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_sec_app_bar)

        lockSetting = view.findViewById(R.id.lock_setting)
        lockTimeoutSetting = view.findViewById(R.id.lock_timeout_setting)

        setUpLockTypeText()
        setUpLockTimeoutText()

        val lockSettingButton = view.findViewById<RelativeLayout>(R.id.lock_settings_button)
        lockSettingButton.setOnClickListener { goToUnlockingActivity() }

        val lockTimeoutSettingButton = view.findViewById<RelativeLayout>(R.id.lock_timeout_settings_button)
        lockTimeoutSettingButton.setOnClickListener { showLockTimeoutSettingDialog() }

        val deleteVault = view.findViewById<CheckBox>(R.id.delete_vault)
        deleteVault.isChecked = Preferences.isUninstallOnPanic()
        deleteVault.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            deleteVault.isChecked = isChecked
            Preferences.setEraseForms(isChecked)
        }

        val deleteForms = view.findViewById<CheckBox>(R.id.delete_forms)
        deleteForms.isChecked = Preferences.isEraseForms()
        deleteForms.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            deleteForms.isChecked = isChecked
            Preferences.setEraseForms(isChecked)
        }

        val deleteServerSettings = view.findViewById<CheckBox>(R.id.delete_server_settings)
        deleteServerSettings.isChecked = Preferences.isDeleteServerSettingsActive()
        deleteServerSettings.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            deleteServerSettings.isChecked = isChecked
            Preferences.setDeleteServerSettingsActive(isChecked)
        }

        val deleteTella = view.findViewById<CheckBox>(R.id.delete_tella)
        deleteTella.isChecked = Preferences.isUninstallOnPanic()
        deleteTella.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            deleteTella.isChecked = isChecked
            Preferences.setUninstallOnPanic(isChecked)
        }

        val quickExitTellaSwitch = view.findViewById<TellaSwitchWithMessage>(R.id.quick_delete_switch)
        setupQuickExitSwitch(quickExitTellaSwitch.mSwitch, view)
        setupQuickExitSettingsView(quickExitTellaSwitch.mSwitch, view)

        val silentCameraTellaSwitch = view.findViewById<TellaSwitchWithMessage>(R.id.camera_silent_switch)
        silentCameraTellaSwitch.mSwitch.setChecked(!Preferences.isShutterMute())
        silentCameraTellaSwitch.mSwitch.setOnCheckedChangeListener({ buttonView: CompoundButton?, isChecked: Boolean ->
            Preferences.setShutterMute(isChecked)
        })

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
        setUpLockTimeoutText()
    }

    private fun setUpLockTypeText() {
        when ((activity?.getApplicationContext() as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(activity)) {
            UnlockRegistry.Method.TELLA_PIN -> lockSetting?.setText(getString(R.string.onboard_pin))
            UnlockRegistry.Method.TELLA_PASSWORD -> lockSetting?.setText(getString(R.string.onboard_password))
            UnlockRegistry.Method.TELLA_PATTERN -> lockSetting?.setText(getString(R.string.onboard_pattern))
        }
    }

    private fun setUpLockTimeoutText() {
        lockTimeoutSetting?.setText(lockTimeoutManager.selectedStringRes)
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

    private fun setupQuickExitSwitch(quickExitSwitch: SwitchCompat, view: View) {
        quickExitSwitch.setOnCheckedChangeListener({ buttonView: CompoundButton?, isChecked: Boolean ->
            Preferences.setQuickExit(isChecked)
            setupQuickExitSettingsView(quickExitSwitch, view)
        })
    }

    private fun setupQuickExitSettingsView(quickExitSwitch: SwitchCompat, view: View) {
        val quickExitSettings = view.findViewById<View>(R.id.quick_exit_settings_layout)
        if (Preferences.isQuickExit()) {
            quickExitSwitch.setChecked(true)
            quickExitSettings.setVisibility(View.VISIBLE)
            /*if (numOfCollectServers == 0L) {
                deleteFormsView.setVisibility(View.GONE)
                deleteSettingsView.setVisibility(View.GONE)
            }*/
        } else {
            quickExitSwitch.setChecked(false)
            quickExitSettings.setVisibility(View.GONE)
        }
    }
}
