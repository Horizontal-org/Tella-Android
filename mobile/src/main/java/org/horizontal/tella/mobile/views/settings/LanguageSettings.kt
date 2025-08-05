package org.horizontal.tella.mobile.views.settings

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.event.LocaleChangedEvent
import org.horizontal.tella.mobile.util.LocaleManager
import org.horizontal.tella.mobile.util.StringUtils
import org.horizontal.tella.mobile.views.base_ui.BaseFragment
import java.util.*
import kotlin.collections.ArrayList


class LanguageSettings : BaseFragment(), View.OnClickListener {
    var LanguageList: LinearLayout? = null
    var languages: ArrayList<String?> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_language_settings, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (baseActivity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_lang_app_bar)
        (baseActivity as OnFragmentSelected?)?.setToolbarHomeIcon(R.drawable.ic_close_white_24dp)

        LanguageList = view.findViewById(R.id.language_list)

        createLangViews()
    }

    private fun createLangViews() {
        if (languages.isEmpty()) {
            languages =
                ArrayList(listOf(*resources.getStringArray(R.array.ra_lang_codes)))
            languages.add(0, null)

            val prefferedLang = LocaleManager.getInstance().languageSetting

            for (language in languages) {
                val item = getLanguageItem(language, TextUtils.equals(prefferedLang, language))
                item.setOnClickListener(this)
                LanguageList!!.addView(item)
            }
        }
    }

    private fun getLanguageItem(language: String?, selected: Boolean): View {
        val inflater = LayoutInflater.from(requireContext())
        @SuppressLint("InflateParams") val item: FrameLayout =
            inflater.inflate(R.layout.language_item_layout, null) as FrameLayout

        val langName: TextView = item.findViewById(R.id.lang)
        val langInfo: TextView = item.findViewById(R.id.lang_info)
        val imageView: ImageView = item.findViewById(R.id.language_check)

        item.tag = language

        if (language == null) {
            langName.setText(R.string.settings_lang_select_default)
            langInfo.setText(R.string.settings_lang_select_expl_default)
        } else {
            // Properly handle locales with language-region codes
            val locale = if (language.contains("-")) {
                val parts = language.split("-")
                Locale(parts[0], parts[1]) // Example: "pt-MZ" → Locale("pt", "MZ")
            } else {
                Locale(language) // Example: "fr" → Locale("fr")
            }

            langName.text = StringUtils.capitalize(locale.displayName, locale)
            langInfo.text = StringUtils.capitalize(locale.getDisplayName(locale), locale)
        }

        imageView.visibility = if (selected) View.VISIBLE else View.GONE
        item.setBackgroundColor(
            if (selected) ContextCompat.getColor(requireContext(), R.color.light_purple)
            else ContextCompat.getColor(requireContext(), R.color.dark_purple)
        )

        return item
    }


    override fun onClick(v: View) {
        setAppLanguage(v.tag as String?)
    }

    private fun setAppLanguage(language: String?) {
        val locale = if (!language.isNullOrEmpty()) {
            if (language.contains("-")) {
                // Split the language code into parts (e.g., "pt-MZ" → "pt" and "MZ")
                val parts = language.split("-")
                Locale(parts[0], parts[1]) // Create a Locale with language and region
            } else {
                Locale(language) // Create a Locale with just the language (e.g., "pt")
            }
        } else {
            null // Handle the default language case
        }

        locale?.let {
            Locale.setDefault(it) // Set globally
            val config = Configuration()
            config.setLocale(it) // Apply to config
            config.setLayoutDirection(it)
        }

        LocaleManager.getInstance().setLocale(locale)
        MyApplication.bus().post(LocaleChangedEvent(locale))
    }

}