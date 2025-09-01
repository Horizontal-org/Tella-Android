package org.horizontal.tella.mobile.views.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.switches.TellaSwitchWithMessage
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.sharedpref.Preferences
import org.horizontal.tella.mobile.data.sharedpref.Preferences.hasAcceptedAnalytics
import org.horizontal.tella.mobile.databinding.FragmentGeneralSettingsBinding
import org.horizontal.tella.mobile.util.C.LOCATION_PERMISSION
import org.horizontal.tella.mobile.util.LocaleManager
import org.horizontal.tella.mobile.util.StringUtils
import org.horizontal.tella.mobile.util.ThemeStyleManager
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.views.activity.analytics.AnalyticsActions
import org.horizontal.tella.mobile.views.activity.analytics.AnalyticsIntroActivity
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import java.util.Locale


class GeneralSettings :
    BaseBindingFragment<FragmentGeneralSettingsBinding>(FragmentGeneralSettingsBinding::inflate) {
    private var viewCreated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        viewCreated = true
    }

    private fun initView() {
        (baseActivity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_select_general)
        (baseActivity as OnFragmentSelected?)?.setToolbarHomeIcon(R.drawable.ic_arrow_back_white_24dp)

        binding.languageSettingsButton.setOnClickListener {
            Navigation.findNavController(it)
                .navigate(R.id.action_general_settings_to_language_settings)
        }

        binding.shareDataSwitch.setTextAndAction(R.string.action_learn_more) {
            Util.startBrowserIntent(
                context,
                getString(R.string.config_analytics_learn_url)
            )
        }
        binding.shareDataSwitch.mSwitch.isChecked = hasAcceptedAnalytics()

        setLanguageSetting()

        initSwitch(
            binding.shareDataSwitch,
            Preferences::setIsAcceptedAnalytics
        ) { isChecked ->
            if (isChecked) {
                DialogUtils.showBottomMessage(
                    requireActivity(), getString(R.string.Settings_Analytics_turn_on_dialog), false
                )
                baseActivity.divviupUtils.runInstallEvent()
            } else {
                DialogUtils.showBottomMessage(
                    requireActivity(), getString(R.string.Settings_Analytics_turn_off_dialog), false
                )
            }
        }

        initSwitch(
            binding.crashReportSwitch,
            Preferences::setSubmittingCrashReports
        )

        binding.verificationSwitch.mSwitch.setOnClickListener {
            if (!context?.let { hasLocationPermission(it) }!!) {
                requestLocationPermission(LOCATION_PERMISSION)
            }
            Preferences.setAnonymousMode(!binding.verificationSwitch.mSwitch.isChecked)
        }

        initSwitch(
            binding.favoriteFormsSwitch,
            Preferences::setShowFavoriteForms
        )

        initSwitch(
            binding.favoriteTemplatesSwitch,
            Preferences::setShowFavoriteTemplates
        )

        initSwitch(
            binding.recentFilesSwitch,
            Preferences::setShowRecentFiles
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initSwitch(
                binding.textJustificationSwitch,
                Preferences::setTextJustification
            ) { _ ->
                applyActivityTheme()
                refreshFragment()
            }
        } else {
            binding.textJustificationSwitch.hide()
        }

        initSwitch(
            binding.textSpacingSwitch,
            Preferences::setTextSpacing
        ) { _ ->
            applyActivityTheme()
            refreshFragment()
        }

    }

    private fun initSwitch(
        switchView: TellaSwitchWithMessage,
        preferencesSetter: (Boolean) -> Unit,
        onClickListener: (Boolean) -> Unit = {}
    ) {
        switchView.mSwitch.setOnClickListener {
            try {
                preferencesSetter(switchView.mSwitch.isChecked)
                onClickListener(switchView.mSwitch.isChecked)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateView() {
        binding.recentFilesSwitch.mSwitch.isChecked = Preferences.isShowRecentFiles()
        binding.favoriteTemplatesSwitch.mSwitch.isChecked = Preferences.isShowFavoriteTemplates()
        binding.favoriteFormsSwitch.mSwitch.isChecked = Preferences.isShowFavoriteForms()
        binding.verificationSwitch.mSwitch.isChecked = !Preferences.isAnonymousMode()
        binding.crashReportSwitch.mSwitch.isChecked = Preferences.isSubmittingCrashReports()
        binding.textJustificationSwitch.mSwitch.isChecked = Preferences.isTextJustification()
        binding.textSpacingSwitch.mSwitch.isChecked = Preferences.isTextSpacing()
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    private fun setLanguageSetting() {
        val language = LocaleManager.getInstance().languageSetting
        if (language != null) {
            val locale = if (language.contains("-")) {
                val parts = language.split("-")
                Locale(parts[0], parts[1]) // e.g., "sn-ZW" → Locale("sn", "ZW")
            } else {
                Locale(language)
            }

            val displayLang = locale.getDisplayLanguage(locale)
            val displayCountry = locale.getDisplayCountry(locale)

            val label = if (displayCountry.isNotEmpty()) {
                StringUtils.capitalize("$displayLang ($displayCountry)", locale)
            } else {
                StringUtils.capitalize(displayLang, locale)
            }

            binding.languageSetting.text = label
        } else {
            binding.languageSetting.setText(R.string.settings_lang_select_default)
        }
    }

        private fun startCleanInsightActivity() {
        val intent = Intent(context, AnalyticsIntroActivity::class.java)
        startActivityForResult(intent, AnalyticsIntroActivity.CLEAN_INSIGHTS_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AnalyticsIntroActivity.CLEAN_INSIGHTS_REQUEST_CODE) {
            val analyticsActions =
                data?.extras?.getSerializable(AnalyticsIntroActivity.RESULT_FOR_ACTIVITY) as AnalyticsActions
            showMessageForCleanInsightsApprove(analyticsActions)
        }
    }

    private fun showMessageForCleanInsightsApprove(analyticsActions: AnalyticsActions) {
        when (analyticsActions) {
            AnalyticsActions.YES -> {
                Preferences.setIsAcceptedAnalytics(true)
                binding.shareDataSwitch.mSwitch.isChecked = true
                DialogUtils.showBottomMessage(
                    requireActivity(), getString(R.string.Settings_Analytics_turn_on_dialog), false
                )
            }

            AnalyticsActions.NO -> {
                Preferences.setIsAcceptedAnalytics(false)
                binding.shareDataSwitch.mSwitch.isChecked = false
                DialogUtils.showBottomMessage(
                    requireActivity(), getString(R.string.Settings_Analytics_turn_off_dialog), false
                )
            }

            else -> {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewCreated = false
    }

    private fun applyActivityTheme() {
        if (viewCreated) {
            activity?.theme?.applyStyle(ThemeStyleManager.getThemeStyle(baseActivity), true)
        }
    }

    private fun refreshFragment() {
        if (viewCreated) {
            BottomSheetUtils.showWarningSheetWithImageAndTimeout(
                baseActivity.supportFragmentManager,
                getString(R.string.Settings_General_BottomSheetRefreshWarningTitle),
                getString(R.string.Settings_General_BottomSheetRefreshWarningText),
                ContextCompat.getDrawable(baseActivity, R.drawable.refresh_phone_device),
                consumer = object : BottomSheetUtils.ActionConfirmed {
                    override fun accept(isConfirmed: Boolean) {
                        nav().popBackStack()
                    }
                },
                BottomSheetUtils.SHORT_TIMEOUT
            )
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        baseActivity.maybeChangeTemporaryTimeout()
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission(requestCode: Int) {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ), requestCode
        )
    }
}