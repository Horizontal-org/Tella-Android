package rs.readahead.washington.mobile.views.settings

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.RETURN_ACTIVITY
import com.hzontal.tella_locking_ui.ReturnActivity
import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentSecuritySettingsBinding
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import timber.log.Timber


class SecuritySettings : BaseFragment() {

    private val lockTimeoutManager = LockTimeoutManager()
    private val cm = CamouflageManager.getInstance()
    private var binding: FragmentSecuritySettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSecuritySettingsBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.showAppbar()
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_sec_app_bar)

        setUpLockTimeoutText()

        val camouflageSettingButton = binding?.camouflageSettingsButton
        if (camouflageSettingButton != null) {
            camouflageSettingButton.setOnClickListener { goToUnlockingActivity(ReturnActivity.CAMOUFLAGE) }
        }

        val currentCamouflageSetting = binding?.camouflageSetting
        if (cm.getLauncherName(requireContext()) != null) {
            if (currentCamouflageSetting != null) {
                currentCamouflageSetting.setText(cm.getLauncherName(requireContext()))
            };
        }

        val lockSettingButton = binding?.lockSettingsButton
        if (lockSettingButton != null) {
            lockSettingButton.setOnClickListener { checkCamouflageAndLockSetting() }
        }

        val lockTimeoutSettingButton = binding?.lockTimeoutSettingsButton
        if (lockTimeoutSettingButton != null) {
            lockTimeoutSettingButton.setOnClickListener { showLockTimeoutSettingDialog() }
        }

        val deleteVault = binding?.deleteVault
        if (deleteVault != null) {
            deleteVault.isChecked = Preferences.isUninstallOnPanic()
            deleteVault.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                deleteVault.isChecked = isChecked
                Preferences.setEraseForms(isChecked)
            }
        }

        val deleteForms = binding?.deleteForms
        if (deleteForms != null) {
            deleteForms.isChecked = Preferences.isEraseForms()
            deleteForms.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
                deleteForms.isChecked = isChecked
                Preferences.setEraseForms(isChecked)
            }
        }

        val deleteServerSettings = binding?.deleteServerSettings
        if (deleteServerSettings != null) {
            deleteServerSettings.isChecked = Preferences.isDeleteServerSettingsActive()
            deleteServerSettings.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
                deleteServerSettings.isChecked = isChecked
                Preferences.setDeleteServerSettingsActive(isChecked)
            }
        }

        val deleteTella = binding?.deleteTella
        if (deleteTella != null) {
            deleteTella.isChecked = Preferences.isUninstallOnPanic()
            deleteTella.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
                deleteTella.isChecked = isChecked
                Preferences.setUninstallOnPanic(isChecked)
            }
        }

        val quickExitTellaSwitch = binding?.quickDeleteSwitch
        if (quickExitTellaSwitch != null) {
            setupQuickExitSwitch(quickExitTellaSwitch.mSwitch, view)
            setupQuickExitSettingsView(quickExitTellaSwitch.mSwitch, view)
        }

        val silentCameraTellaSwitch = binding?.cameraSilentSwitch
        if (silentCameraTellaSwitch != null) {
            silentCameraTellaSwitch.mSwitch.isChecked = Preferences.isShutterMute()
            silentCameraTellaSwitch.mSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                Preferences.setShutterMute(isChecked)
            }
        }

        val enableSecurityScreen = binding?.securityScreenSwitch
        if (enableSecurityScreen != null) {
            enableSecurityScreen.mSwitch.isChecked = Preferences.isSecurityScreenEnabled()
            enableSecurityScreen.mSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                enableSecurityScreen.isChecked = isChecked
                Preferences.setSecurityScreenEnabled(isChecked)
            }
        }

        /*val bypassCensorshipTellaSwitch =
            view.findViewById<TellaSwitchWithMessage>(R.id.bypass_censorship_switch)
        bypassCensorshipTellaSwitch.mSwitch.setChecked(Preferences.isBypassCensorship())
        bypassCensorshipTellaSwitch.mSwitch.setOnCheckedChangeListener({ buttonView: CompoundButton?, isChecked: Boolean ->
            Preferences.setBypassCensorship(isChecked)
        })*/

        binding?.deleteVaultTooltip?.setOnClickListener {
            showTooltip(
                binding?.deleteVaultTooltip!!,
                resources.getString(R.string.settings_sec_delete_vault_tooltip),
                Gravity.TOP
            )
        }

        binding?.deleteFormsTooltip?.setOnClickListener {
            showTooltip(
                binding?.deleteFormsTooltip!!,
                resources.getString(R.string.settings_sec_delete_forms_tooltip),
                Gravity.TOP
            )
        }

        binding?.deleteServerTooltip?.setOnClickListener {
            showTooltip(
                binding?.deleteServerTooltip!!,
                resources.getString(R.string.settings_sec_delete_servers_tooltip),
                Gravity.TOP
            )
        }

        binding?.deleteAppTooltip?.setOnClickListener {
            showTooltip(
                binding?.deleteAppTooltip!!,
                resources.getString(R.string.settings_sec_delete_app_tooltip),
                Gravity.TOP
            )
        }
    }

    override fun onStart() {
        super.onStart()
        setUpLockTypeText()
    }

    private fun showLockTimeoutSettingDialog() {
        val optionConsumer = object : BottomSheetUtils.LockOptionConsumer {
            override fun accept(option: Long) {
                onLockTimeoutChoosen(option)
            }
        }
        activity.let {
            BottomSheetUtils.showRadioListSheet(
                requireActivity().supportFragmentManager,
                requireContext(),
                lockTimeoutManager.lockTimeout,
                lockTimeoutManager.optionsList,
                getString(R.string.settings_select_lock_timeout),
                getString(R.string.settings_sec_lock_timeout_desc),
                getString(R.string.action_ok),
                getString(R.string.action_cancel),
                optionConsumer
            )
        }
    }

    private fun onLockTimeoutChoosen(option: Long) {
        lockTimeoutManager.lockTimeout = option
        setUpLockTimeoutText()
    }

    private fun setUpLockTypeText() {
        when ((activity.applicationContext as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
            activity
        )) {
            UnlockRegistry.Method.TELLA_PIN -> binding?.lockSetting?.setText(getString(R.string.onboard_pin))
            UnlockRegistry.Method.TELLA_PASSWORD -> binding?.lockSetting?.setText(getString(R.string.onboard_password))
            UnlockRegistry.Method.TELLA_PATTERN -> binding?.lockSetting?.setText(getString(R.string.onboard_pattern))
            else -> {
                Timber.e("Unlock method not recognized")
            }
        }
    }

    private fun setUpLockTimeoutText() {
        binding?.lockTimeoutSetting?.setText(lockTimeoutManager.selectedStringRes)
    }

    private fun checkCamouflageAndLockSetting() {
        if ((activity.getApplicationContext() as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
                activity
            ) == UnlockRegistry.Method.TELLA_PIN
            && Preferences.getAppAlias().equals(cm.calculatorOption.alias)
        ) {
            showConfirmSheet(
                requireActivity().supportFragmentManager,
                null,
                getString(R.string.settings_sec_change_lock_type_warning),
                getString(R.string.action_continue),
                getString(R.string.action_cancel),
                object : ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) {
                        if (isConfirmed) {
                            goToUnlockingActivity(ReturnActivity.SETTINGS)
                        }
                    }
                })
        } else {
            goToUnlockingActivity(ReturnActivity.SETTINGS)
        }
    }

    fun goToUnlockingActivity(returnCall: ReturnActivity) {
        var intent: Intent? = null
        when ((activity.getApplicationContext() as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
            activity
        )) {
            UnlockRegistry.Method.TELLA_PIN -> intent =
                Intent(activity, PinUnlockActivity::class.java)
            UnlockRegistry.Method.TELLA_PASSWORD -> intent =
                Intent(activity, PasswordUnlockActivity::class.java)
            UnlockRegistry.Method.TELLA_PATTERN -> intent =
                Intent(activity, PatternUnlockActivity::class.java)
            else -> {
                Timber.e("Unlock method not recognized")
            }
        }

        intent!!.putExtra(RETURN_ACTIVITY, returnCall.getActivityOrder())
        intent.putExtra(IS_FROM_SETTINGS, true)
        startActivity(intent)
        activity.finish()
    }

    private fun setupQuickExitSwitch(quickExitSwitch: SwitchCompat, view: View) {
        quickExitSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            Preferences.setQuickExit(isChecked)
            setupQuickExitSettingsView(quickExitSwitch, view)
        }
    }

    private fun setupQuickExitSettingsView(quickExitSwitch: SwitchCompat, view: View) {
        if (binding?.quickExitSettingsLayout == null) return
        if (Preferences.isQuickExit()) {
            quickExitSwitch.setChecked(true)
            binding?.quickExitSettingsLayout!!.setVisibility(View.VISIBLE)
            /*if (numOfCollectServers == 0L) {
                deleteFormsView.setVisibility(View.GONE)
                deleteSettingsView.setVisibility(View.GONE)
            }*/
        } else {
            quickExitSwitch.setChecked(false)
            binding?.quickExitSettingsLayout!!.setVisibility(View.GONE)
        }
    }
}
