package rs.readahead.washington.mobile.views.settings

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.RETURN_ACTIVITY
import com.hzontal.tella_locking_ui.ReturnActivity
import com.hzontal.tella_locking_ui.TellaKeysUI
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
import rs.readahead.washington.mobile.util.FailedUnlockManager
import rs.readahead.washington.mobile.util.LockTimeoutManager
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import timber.log.Timber


class SecuritySettings : BaseFragment() {

    private val lockTimeoutManager by lazy { LockTimeoutManager() }
    private val failedUnlockManager by lazy { FailedUnlockManager() }
    private val cm = CamouflageManager.getInstance()
    private lateinit var binding: FragmentSecuritySettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSecuritySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        val fragmentSelected = baseActivity as OnFragmentSelected?
        fragmentSelected?.showAppbar()
        fragmentSelected?.setToolbarLabel(R.string.settings_sec_app_bar)
        setUpDeleteAfterUnlockText()
        setUpLockTimeoutText()
        binding.camouflageSettingsButton.setOnClickListener { goToUnlockingActivity(ReturnActivity.CAMOUFLAGE) }
        if (cm.getLauncherName(baseActivity) != null) {
            binding.camouflageSetting.text = cm.getLauncherName(baseActivity)
        }

        binding.lockSettingsButton.setOnClickListener { checkCamouflageAndLockSetting() }

        binding.lockTimeoutSettingsButton.setOnClickListener { showLockTimeoutSettingDialog() }

        binding.deleteUnlockSettingsButton.setOnClickListener { showDeleteAfterFailedUnlockDialog() }


        setupCheckedChangeListener(
            binding.deleteVault, Preferences.isDeleteGalleryEnabled()
        ) { isChecked ->
            binding.deleteVault.isChecked = isChecked
            Preferences.setDeleteGallery(isChecked)
        }

        setupCheckedChangeListener(binding.deleteForms, Preferences.isEraseForms()) { isChecked ->
            binding.deleteForms.isChecked = isChecked
            Preferences.setEraseForms(isChecked)
        }

        setupCheckedChangeListener(
            binding.deleteServerSettings, Preferences.isDeleteServerSettingsActive()
        ) { isChecked ->
            binding.deleteServerSettings.isChecked = isChecked
            Preferences.setDeleteServerSettingsActive(isChecked)
        }

        setupCheckedChangeListener(
            binding.deleteTella, Preferences.isUninstallOnPanic()
        ) { isChecked ->
            binding.deleteTella.isChecked = isChecked
            Preferences.setUninstallOnPanic(isChecked)
        }


        val quickExitTellaSwitch = binding.quickDeleteSwitch
        setupQuickExitSwitch(quickExitTellaSwitch.mSwitch)
        setupQuickExitSettingsView(quickExitTellaSwitch.mSwitch)

        val silentCameraTellaSwitch = binding.cameraSilentSwitch
        silentCameraTellaSwitch.mSwitch.isChecked = Preferences.isShutterMute()
        silentCameraTellaSwitch.mSwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.setShutterMute(isChecked)
            }
        }

        val enableSecurityScreen = binding.securityScreenSwitch
        enableSecurityScreen.mSwitch.isChecked = Preferences.isSecurityScreenEnabled()
        enableSecurityScreen.mSwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.setSecurityScreenEnabled(isChecked)
            }
        }

        val keepExifTellaSwitch = binding.keepExifSwitch
        keepExifTellaSwitch.mSwitch.isChecked = Preferences.isKeepExif()
        keepExifTellaSwitch.mSwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.setKeepExif(isChecked)
            }
        }


        /*val bypassCensorshipTellaSwitch =
            view.findViewById<TellaSwitchWithMessage>(R.id.bypass_censorship_switch)
        bypassCensorshipTellaSwitch.mSwitch.setChecked(Preferences.isBypassCensorship())
        bypassCensorshipTellaSwitch.mSwitch.setOnCheckedChangeListener({ buttonView: CompoundButton?, isChecked: Boolean ->
            Preferences.setBypassCensorship(isChecked)
        })*/

        binding.deleteVaultTooltip.setOnClickListener {
            showTooltip(
                binding.deleteVaultTooltip,
                resources.getString(R.string.settings_sec_delete_vault_tooltip),
                Gravity.TOP
            )
        }

        binding.deleteFormsTooltip.setOnClickListener {
            showTooltip(
                binding.deleteFormsTooltip,
                resources.getString(R.string.settings_sec_delete_forms_tooltip),
                Gravity.TOP
            )
        }

        binding.deleteServerTooltip.setOnClickListener {
            showTooltip(
                binding.deleteServerTooltip,
                resources.getString(R.string.settings_sec_delete_servers_tooltip),
                Gravity.TOP
            )
        }

        binding.deleteAppTooltip.setOnClickListener {
            showTooltip(
                binding.deleteAppTooltip,
                resources.getString(R.string.settings_sec_delete_app_tooltip),
                Gravity.TOP
            )
        }
    }

    private fun setupCheckedChangeListener(
        switch: CheckBox, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit
    ) {
        switch.isChecked = isChecked
        switch.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(isChecked)
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
        baseActivity.let {
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

    private fun showDeleteAfterFailedUnlockDialog() {
        val optionConsumer = object : BottomSheetUtils.LockOptionConsumer {
            override fun accept(option: Long) {
                onFailedAttemptChoosen(option)
            }
        }
        baseActivity.let {
            BottomSheetUtils.showRadioListSheet(
                requireActivity().supportFragmentManager,
                baseActivity,
                failedUnlockManager.getLockTimeout(),
                failedUnlockManager.getOptionsList(),
                getString(R.string.Settings_Delete_After_Failed_Unlock),
                getString(R.string.Settings_Delete_After_Failed_Unlock_Descreption),
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
        when ((baseActivity.applicationContext as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
            baseActivity
        )) {
            UnlockRegistry.Method.TELLA_PIN -> binding.lockSetting.text =
                getString(R.string.onboard_pin)

            UnlockRegistry.Method.TELLA_PASSWORD -> binding.lockSetting.text =
                getString(R.string.onboard_password)

            UnlockRegistry.Method.TELLA_PATTERN -> binding.lockSetting.text =
                getString(R.string.onboard_pattern)

            else -> {
                Timber.e("Unlock method not recognized")
            }
        }
    }

    private fun setUpLockTimeoutText() {
        binding.lockTimeoutSetting.setText(lockTimeoutManager.selectedStringRes)
    }

    private fun onFailedAttemptChoosen(option: Long) {
        failedUnlockManager.setFailedUnlockOption(option)
        TellaKeysUI.setNumFailedAttempts(option)
        setUpDeleteAfterUnlockText()
    }

    private fun setUpDeleteAfterUnlockText() {
        binding.deleteUnlockSetting.setText(failedUnlockManager.getFailedUnlockOption())
    }

    private fun checkCamouflageAndLockSetting() {
        if ((baseActivity.applicationContext as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
                baseActivity
            ) == UnlockRegistry.Method.TELLA_PIN && Preferences.getAppAlias()
                .equals(cm.getCalculatorOptionByTheme(Preferences.getCalculatorTheme()).alias)
        ) {
            showConfirmSheet(requireActivity().supportFragmentManager,
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
        when ((baseActivity.applicationContext as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
            baseActivity
        )) {
            UnlockRegistry.Method.TELLA_PIN -> intent =
                Intent(baseActivity, PinUnlockActivity::class.java)

            UnlockRegistry.Method.TELLA_PASSWORD -> intent =
                Intent(baseActivity, PasswordUnlockActivity::class.java)

            UnlockRegistry.Method.TELLA_PATTERN -> intent =
                Intent(baseActivity, PatternUnlockActivity::class.java)

            else -> {
                Timber.e("Unlock method not recognized")
            }
        }

        intent?.putExtra(RETURN_ACTIVITY, returnCall.getActivityOrder())
        intent?.putExtra(IS_FROM_SETTINGS, true)
        startActivity(intent)
        baseActivity.finish()
    }

    private fun setupQuickExitSwitch(quickExitSwitch: SwitchCompat) {
        quickExitSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            Preferences.setQuickExit(isChecked)
            setupQuickExitSettingsView(quickExitSwitch)
        }
    }

    private fun setupQuickExitSettingsView(quickExitSwitch: SwitchCompat) {
        if (Preferences.isQuickExit()) {
            quickExitSwitch.isChecked = true
            binding.quickExitSettingsLayout.visibility = View.VISIBLE/*if (numOfCollectServers == 0L) {
                deleteFormsView.setVisibility(View.GONE)
                deleteSettingsView.setVisibility(View.GONE)
            }*/
        } else {
            quickExitSwitch.isChecked = false
            binding.quickExitSettingsLayout.visibility = View.GONE
        }
    }
}
