package rs.readahead.washington.mobile.views.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.app.ActivityCompat
import androidx.navigation.Navigation
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.databinding.FragmentGeneralSettingsBinding
import rs.readahead.washington.mobile.util.C.LOCATION_PERMISSION
import rs.readahead.washington.mobile.util.CleanInsightUtils
import rs.readahead.washington.mobile.util.LocaleManager
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActions
import rs.readahead.washington.mobile.views.activity.clean_insights.CleanInsightsActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import java.util.*


class GeneralSettings : BaseFragment() {

    private var binding: FragmentGeneralSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGeneralSettingsBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_select_general)
        (activity as OnFragmentSelected?)?.setToolbarHomeIcon(R.drawable.ic_arrow_back_white_24dp)

        binding?.languageSettingsButton?.setOnClickListener {
            Navigation.findNavController(view)
                .navigate(R.id.action_general_settings_to_language_settings)
        }

        setLanguageSetting()

        binding?.shareDataSwitch.let {
            if (it != null) {
                it.mSwitch.isChecked = Preferences.hasAcceptedImprovements()
                it.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
                    try {
                        Preferences.setIsAcceptedImprovements(isChecked)
                        CleanInsightUtils.grantCampaign(isChecked)
                        if (isChecked) showMessageForCleanInsightsApprove(CleanInsightsActions.YES)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                it.setTextAndAction(R.string.action_learn_more) { startCleanInsightActivity() }
            }
        }

        val crashReportsSwitch = binding?.crashReportSwitch
        if (crashReportsSwitch != null) {
            crashReportsSwitch.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
                Preferences.setSubmittingCrashReports(isChecked)
            }
            crashReportsSwitch.mSwitch.isChecked = Preferences.isSubmittingCrashReports()
        }

        val verificationSwitch = binding?.verificationSwitch
        if (verificationSwitch != null) {
            verificationSwitch.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
                run {
                    if (!context?.let { hasLocationPermission(it) }!!) {
                        requestLocationPermission(LOCATION_PERMISSION)
                    }
                    Preferences.setAnonymousMode(!isChecked)
                }
            }
            verificationSwitch.mSwitch.isChecked = !Preferences.isAnonymousMode()
        }

        val favoriteFormsSwitch = binding?.favoriteFormsSwitch
        if (favoriteFormsSwitch != null) {
            favoriteFormsSwitch.mSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                Preferences.setShowFavoriteForms(isChecked)
            }
            favoriteFormsSwitch.mSwitch.isChecked = Preferences.isShowFavoriteForms()
        }

        val favoriteTemplatesSwitch = binding?.favoriteTemplatesSwitch
        if (favoriteTemplatesSwitch != null) {
            favoriteTemplatesSwitch.mSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                Preferences.setShowFavoriteTemplates(isChecked)
            }
            favoriteTemplatesSwitch.mSwitch.isChecked = Preferences.isShowFavoriteTemplates()
        }

        val recentFilesSwitch = binding?.recentFilesSwitch
        if (recentFilesSwitch != null) {
            recentFilesSwitch.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
                Preferences.setShowRecentFiles(isChecked)
            }

            recentFilesSwitch.mSwitch.isChecked = Preferences.isShowRecentFiles()
        }
    }

    private fun setLanguageSetting() {
        val language = LocaleManager.getInstance().languageSetting
        if (language != null) {
            val locale = Locale(language)
            binding?.languageSetting?.setText(StringUtils.capitalize(locale.displayName, locale))
        } else {
            binding?.languageSetting?.setText(R.string.settings_lang_select_default)
        }
    }

    private fun startCleanInsightActivity() {
        val intent = Intent(context, CleanInsightsActivity::class.java)
        startActivityForResult(intent, CleanInsightsActivity.CLEAN_INSIGHTS_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CleanInsightsActivity.CLEAN_INSIGHTS_REQUEST_CODE) {
            val cleanInsightsActions =
                data?.extras?.getSerializable(CleanInsightsActivity.RESULT_FOR_ACTIVITY) as CleanInsightsActions
            showMessageForCleanInsightsApprove(cleanInsightsActions)
        }
    }

    private fun showMessageForCleanInsightsApprove(cleanInsightsActions: CleanInsightsActions) {
        when (cleanInsightsActions) {
            CleanInsightsActions.YES -> {
                Preferences.setIsAcceptedImprovements(true)
                CleanInsightUtils.grantCampaign(true)
                binding?.shareDataSwitch?.mSwitch?.isChecked = true
                DialogUtils.showBottomMessage(
                    requireActivity(),
                    getString(R.string.clean_insights_signed_for_days),
                    false
                )
            }
            CleanInsightsActions.NO -> {
                Preferences.setIsAcceptedImprovements(false)
                CleanInsightUtils.grantCampaign(false)
                binding?.shareDataSwitch?.mSwitch?.isChecked = false
            }
            else -> {}
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        activity.maybeChangeTemporaryTimeout()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            return true
        return false
    }

    private fun requestLocationPermission(requestCode: Int) {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ), requestCode
        )
    }
}