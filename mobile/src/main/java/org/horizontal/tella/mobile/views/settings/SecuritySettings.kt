package org.horizontal.tella.mobile.views.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.hzontal.tella_locking_ui.IS_FROM_SETTINGS
import com.hzontal.tella_locking_ui.RETURN_ACTIVITY
import com.hzontal.tella_locking_ui.ReturnActivity
import com.hzontal.tella_locking_ui.TellaKeysUI
import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.databinding.FragmentSecuritySettingsBinding
import org.horizontal.tella.mobile.util.CamouflageManager
import org.horizontal.tella.mobile.util.FailedUnlockManager
import org.horizontal.tella.mobile.util.LockTimeoutManager
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionConfirmed
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showConfirmSheet
import org.hzontal.shared_ui.utils.DialogUtils
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import timber.log.Timber


class SecuritySettings :
    BaseBindingFragment<FragmentSecuritySettingsBinding>(FragmentSecuritySettingsBinding::inflate) {

    companion object {
        const val ARG_OPEN_LOCK_CHANGE_FLOW = "open_lock_change_flow"

        /** When true, the lock re-auth / change-type flow starts as soon as the screen opens (e.g. from Hide Tella). */
        @JvmStatic
        fun newInstance(openLockChangeFlow: Boolean = false) = SecuritySettings().apply {
            arguments = bundleOf(ARG_OPEN_LOCK_CHANGE_FLOW to openLockChangeFlow)
        }
    }

    private val lockTimeoutManager by lazy { LockTimeoutManager() }
    private val failedUnlockManager by lazy { FailedUnlockManager() }
    private val cm = CamouflageManager.getInstance()
    private var isUpdatingShutterMuteSwitch = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val openLockChangeFlow = arguments?.getBoolean(ARG_OPEN_LOCK_CHANGE_FLOW) == true
        initView()
        if (openLockChangeFlow) {
            arguments?.putBoolean(ARG_OPEN_LOCK_CHANGE_FLOW, false)
            view.post { checkCamouflageAndLockSetting() }
        }
    }

    private fun initView() {
        val fragmentSelected = baseActivity as OnFragmentSelected?
        fragmentSelected?.showAppbar()
        fragmentSelected?.setToolbarLabel(R.string.settings_sec_app_bar)
        setUpSettingsVisibility()
        setUpLockTimeoutText()
        binding.lockSettingsButton.setOnClickListener { checkCamouflageAndLockSetting() }

        binding.lockTimeoutSettingsButton.setOnClickListener { showLockTimeoutSettingDialog() }

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
        setUpSilentCameraSwitch()

        val enableSecurityScreen = binding.securityScreenSwitch
        enableSecurityScreen.mSwitch.isChecked = Preferences.isSecurityScreenEnabled()
        enableSecurityScreen.mSwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                Preferences.setSecurityScreenEnabled(isChecked)
            }
        }

        binding.unlockRemainingSwitch.mSwitch.isChecked =
            failedUnlockManager.isShowRemainingAttempts()
        binding.unlockRemainingSwitch.mSwitch.apply {
            setOnCheckedChangeListener { _, isChecked ->
                TellaKeysUI.setIsShowRemainingAttempts(isChecked)
                failedUnlockManager.setShowUnlockRemainingAttempts(isChecked)
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

        // 2025-08-19 (audit / Feature 2 + 3): wire the new privacy section.
        // Lives below the existing switches; safe to leave the existing
        // initView() flow intact.
        setupAuditSecuritySection()

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

        hideDeleteTellaCheckBox()
    }


    private fun hideDeleteTellaCheckBox() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            // Check if the SDK version is 34 or higher
            binding.deleteContainer.hide()
        }
    }

    private fun setupCheckedChangeListener(
        switch: CheckBox, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit
    ) {
        switch.apply {
            this.isChecked = isChecked
            setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(isChecked)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setUpLockTypeText()
    }

    override fun onResume() {
        super.onResume()
        syncShutterMuteSwitchState()
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

    private fun setUpSilentCameraSwitch() {
        syncShutterMuteSwitchState()
        binding.cameraSilentSwitch.mSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingShutterMuteSwitch) return@setOnCheckedChangeListener

            Preferences.setShutterMute(isChecked)
        }
    }

    private fun syncShutterMuteSwitchState() {
        setSilentCameraSwitchChecked(Preferences.isShutterMute())
    }

    private fun setSilentCameraSwitchChecked(isChecked: Boolean) {
        isUpdatingShutterMuteSwitch = true
        binding.cameraSilentSwitch.mSwitch.isChecked = isChecked
        isUpdatingShutterMuteSwitch = false
    }

    private fun showDeleteAfterFailedUnlockDialog() {
        val optionConsumer = object : BottomSheetUtils.LockOptionConsumer {
            override fun accept(option: Long) {
                onFailedAttemptChoosen(option)
                if (option > 0L) {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        getString(R.string.Settings_failed_unlock_message_template, option),
                        false
                    )
                }
            }
        }
        baseActivity.let {
            BottomSheetUtils.showRadioListSheet(
                requireActivity().supportFragmentManager,
                baseActivity,
                failedUnlockManager.getOption(),
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
        // Set the lock timeout to the chosen option
        lockTimeoutManager.lockTimeout = option
        // Ensure the temporary timeout flag is reset
        Preferences.setTempTimeout(false)
        // Update the UI or any related components with the new lock timeout value
        setUpLockTimeoutText()
    }


    private fun setUpLockTypeText() {
        when ((baseActivity.applicationContext as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
            baseActivity
        )) {
            UnlockRegistry.Method.TELLA_PIN -> binding.lockSettingsButton.setLabelText(getString(R.string.onboard_pin))

            UnlockRegistry.Method.TELLA_PASSWORD -> binding.lockSettingsButton.setLabelText(
                getString(R.string.onboard_password)
            )

            UnlockRegistry.Method.TELLA_PATTERN -> binding.lockSettingsButton.setLabelText(
                getString(
                    R.string.onboard_pattern
                )
            )

            else -> {
                Timber.e("Unlock method not recognized")
            }
        }
    }

    private fun setUpLockTimeoutText() {
        binding.lockTimeoutSettingsButton.setLabelText(getString(lockTimeoutManager.selectedStringRes))
    }

    private fun onFailedAttemptChoosen(option: Long) {
        failedUnlockManager.setFailedUnlockOption(option)
        TellaKeysUI.setNumFailedAttempts(option)
        TellaKeysUI.setRemainingAttempts(failedUnlockManager.getUnlockRemainingAttempts())
        setUpSettingsVisibility()
    }

    private fun setUpSettingsVisibility() {
        if (!CamouflageManager.getInstance().isDefaultLauncherActivityAlias) {
            setUpNonDefaultLauncherVisibility()
        } else {
            setUpDefaultLauncherVisibility()
        }
    }

    private fun setUpNonDefaultLauncherVisibility() {
        val launcherName = cm.getLauncherName(baseActivity)
        with(binding) {
            unlockRemainingSwitch.isVisible = false
            camouflageSettingsButton.setOnClickListener {
                goToUnlockingActivity(ReturnActivity.CAMOUFLAGE)
            }
            camouflageSettingsButton.setLabelText(launcherName)
            deleteUnlockSettingsButton.apply {
                isBottomLineVisible(failedUnlockManager.getOption() != 0L)
                setInfoText(getString(R.string.Settings_feature_not_available_camouflage))
                setLabelText(getString(R.string.Settings_Disabled))
                setLabelColor(R.color.wa_white_64)
                setOnClickListener(null)
            }
        }
    }

    private fun setUpDefaultLauncherVisibility() {
        with(binding) {
            val failedUnlockOptionLabel = failedUnlockManager.getFailedUnlockOptionText()
            if (failedUnlockManager.getOption() == 0L) {
                setUpCamouflageButtonForDefaultLauncher()
            } else {
                camouflageSettingsButton.setOnClickListener(null)
                camouflageSettingsButton.setLabelText(getString(R.string.Settings_Off))
            }
            unlockRemainingSwitch.isVisible = failedUnlockManager.getOption() != 0L
            deleteUnlockSettingsButton.apply {
                isBottomLineVisible(failedUnlockManager.getOption() != 0L)
                setLabelText(getString(failedUnlockOptionLabel))
                setOnClickListener { showDeleteAfterFailedUnlockDialog() }
                setLabelColor(R.color.wa_white)
            }
        }
    }

    private fun setUpCamouflageButtonForDefaultLauncher() {
        val launcherName = cm.getLauncherName(baseActivity)
        with(binding) {
            camouflageSettingsButton.setOnClickListener {
                goToUnlockingActivity(ReturnActivity.CAMOUFLAGE)
            }
            camouflageSettingsButton.setLabelText(launcherName)
        }
    }


    private fun checkCamouflageAndLockSetting() {
        if ((baseActivity.applicationContext as IUnlockRegistryHolder).unlockRegistry.getActiveMethod(
                baseActivity
            ) == UnlockRegistry.Method.TELLA_PIN && cm.isCalculatorCamouflageActive
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
        intent?.let {
            startActivity(intent)
            baseActivity.finish()
        }
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

    // ====================================================================
    // 2025-08-19 (audit / Feature 2 + 3): new privacy section.
    // Wires Secure Wipe on Import + Quick Delete PIN + Brute-force auto-
    // trigger into the existing SecuritySettings screen. All controls
    // live behind the existing layout's `audit_security_layout` LinearLayout
    // and are invisible to the user until they scroll down past the
    // existing switches — so the existing UI ordering is preserved.
    // ====================================================================
    private fun setupAuditSecuritySection() {
        // ---- Feature 2: Secure Wipe on Import ----
        binding.secureWipeSwitch.mSwitch.isChecked =
            Preferences.isSecureWipeEnabled()
        binding.secureWipeSwitch.mSwitch.setOnCheckedChangeListener { _, isChecked ->
            Preferences.setSecureWipeEnabled(isChecked)
        }

        // ---- Feature 3.A: Quick Delete PIN ----
        binding.quickDeletePinSetting.setOnClickListener {
            // If a PIN is already set, show a small dialog offering
            // Change / Remove / Cancel. Otherwise go straight to Set.
            if (org.hzontal.shared_ui.security.QuickDeletePinManager.isSet(requireContext())) {
                // 2025-08-20 (audit-fix rev 7): use TellaDialogs.builder so
                // the Cancel button on the change/remove picker is visible.
                // Previously this used `AlertDialog.Builder(baseActivity)`
                // which inherited colorAccent = wa_white_80 (invisible).
                org.horizontal.tella.mobile.views.activity.viewer.TellaDialogs
                    .builder(baseActivity)
                    .setTitle(R.string.quick_delete_pin_title)
                    .setItems(arrayOf(
                        getString(R.string.quick_delete_pin_change),
                        getString(R.string.quick_delete_pin_remove)
                    )) { d, idx ->
                        d.dismiss()
                        when (idx) {
                            0 -> org.hzontal.shared_ui.security.QuickDeletePinManager
                                .showSetPinDialog(
                                    baseActivity,
                                    onSaved = {
                                        // 2025-08-19 (audit-fix): refresh the
                                        // brute-force section visibility so the
                                        // user sees the controls appear without
                                        // having to leave and re-enter the screen.
                                        refreshAuditSecuritySectionVisibility()
                                    }
                                )
                            1 -> {
                                org.hzontal.shared_ui.security.QuickDeletePinManager.clearPin(requireContext())
                                android.widget.Toast.makeText(
                                    baseActivity,
                                    R.string.quick_delete_pin_removed,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                // 2025-08-20 (audit-fix rev 8): brute-force
                                // feature removed — no threshold to reset.
                                // Just refresh the audit section visibility.
                                refreshAuditSecuritySectionVisibility()
                            }
                        }
                    }
                    .setNegativeButton(R.string.pdf_annot_dialog_cancel) { d, _ -> d.dismiss() }
                    .show()
            } else {
                org.hzontal.shared_ui.security.QuickDeletePinManager
                    .showSetPinDialog(
                        requireContext(),
                        onSaved = {
                            // 2025-08-20 (audit-fix rev 8): refresh the
                            // quick-delete-pin summary label so it reflects
                            // the new "set" state immediately.
                            refreshAuditSecuritySectionVisibility()
                        }
                    )
            }
        }

        // 2025-08-20 (audit-fix rev 8): the brute-force auto-trigger feature
        // (Feature 3.B/C/D) has been REMOVED. The threshold/window pickers,
        // the toggle, and the refreshBruteForceLabels / showNumberPicker
        // helpers are all deleted. The existing "Delete after failed unlock"
        // feature (see showDeleteAfterFailedUnlockDialog above) already
        // provides the actual protection — brute-force was redundant AND
        // never enforced (the threshold/window were written to prefs but no
        // code read them).
        refreshAuditSecuritySectionVisibility()
    }

    /**
     * 2025-08-20 (audit-fix rev 8): Refreshes the Quick Delete PIN summary
     * label based on whether a PIN is currently set.
     *
     * The brute-force visibility logic that used to live here has been
     * removed along with the feature itself. This method now just updates
     * the quick-delete-pin row's label so the user sees "Set" / "Change"
     * reflect the current state immediately after setting / clearing the
     * PIN, without requiring a screen re-entry.
     */
    private fun refreshAuditSecuritySectionVisibility() {
        val pinSet = org.hzontal.shared_ui.security.QuickDeletePinManager.isSet(requireContext())
        binding.quickDeletePinSetting.setLabelText(
            if (pinSet) getString(R.string.quick_delete_pin_summary_set)
            else getString(R.string.quick_delete_pin_summary)
        )
        // 2026-08-20 (audit-fix rev 11): definitive fix for the "blank area"
        // below the Quick Delete PIN row. Three measures:
        //
        // 1. Hide the bottom line (it was set to visible in rev 7 to
        //    "terminate the card" but it just added a visible dark line
        //    that looked like a blank gap).
        // 2. Set the InfoSettingsView's own padding to 0 on all sides
        //    (the LinearLayout that wraps the internal ConstraintLayout).
        // 3. Find the internal views (centered_linear_layout and
        //    label_textview) and set THEIR bottom padding to 0 — these
        //    have 16dp padding from settings_info_view.xml which is the
        //    actual source of the gap.
        binding.quickDeletePinSetting.isBottomLineVisible(false)
        binding.quickDeletePinSetting.setPadding(0, 0, 0, 0)
        try {
            binding.quickDeletePinSetting.findViewById<android.view.View>(
                org.hzontal.shared_ui.R.id.centered_linear_layout
            )?.setPadding(0, 0, 0, 0)
            binding.quickDeletePinSetting.findViewById<android.view.View>(
                org.hzontal.shared_ui.R.id.label_textview
            )?.setPadding(0, 0, 0, 0)
        } catch (_: Throwable) { /* best-effort — IDs may differ */ }
    }
}
