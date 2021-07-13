package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import org.hzontal.shared_ui.switches.TellaSwitchWithMessage
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.util.LocaleManager
import rs.readahead.washington.mobile.util.StringUtils
import java.util.*


class GeneralSettings : Fragment() {
    var languageSetting: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_general_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_select_general)
        (activity as OnFragmentSelected?)?.setToolbarHomeIcon(R.drawable.ic_arrow_back_white_24dp)

        view.findViewById<View>(R.id.language_settings_button).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_general_settings_to_language_settings)
        }

        languageSetting = view.findViewById(R.id.language_setting)
        setLanguageSetting()

        val crashReportsSwitch = view.findViewById<TellaSwitchWithMessage>(R.id.crash_report_switch)
        crashReportsSwitch.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            Preferences.setSubmittingCrashReports(isChecked)
        }
        crashReportsSwitch.setChecked(Preferences.isSubmittingCrashReports())

        val verificationSwitch = view.findViewById<TellaSwitchWithMessage>(R.id.verification_switch)
        verificationSwitch.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            Preferences.setAnonymousMode(!isChecked)
        }
        verificationSwitch.setChecked(!Preferences.isAnonymousMode())

       /* val languageSettings = view.findViewById<View>(R.id.language_settings_button)
        languageSettings.setOnClickListener {
            activity?.let {
                BottomSheetUtils.showStandardSheet(it.supportFragmentManager,"Add Server",
                        descriptionText = "What type of server?",
                        actionButtonLabel = "confirm",
                        cancelButtonLabel = "cancel",
                        onConfirmClick = { },
                onCancelClick = { })
            }
        }*/

        return view
    }

    private fun setLanguageSetting() {
        val language = LocaleManager.getInstance().languageSetting
        if (language != null) {
            val locale = Locale(language)
            languageSetting?.setText(StringUtils.capitalize(locale.displayName, locale))
        } else {
            languageSetting?.setText(R.string.settings_lang_select_default)
        }
    }
}