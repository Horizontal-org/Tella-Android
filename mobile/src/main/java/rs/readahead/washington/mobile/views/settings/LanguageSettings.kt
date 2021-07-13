package rs.readahead.washington.mobile.views.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Nullable
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.LocaleChangedEvent
import rs.readahead.washington.mobile.util.LocaleManager
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import timber.log.Timber
import java.util.*


class LanguageSettings : BaseFragment(), View.OnClickListener {
    var LanguageList: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_language_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_lang_app_bar)
        (activity as OnFragmentSelected?)?.setToolbarHomeIcon(R.drawable.ic_close_white_24dp)

        LanguageList =view.findViewById(R.id.language_list)
        createLangViews()

        return view
    }

    private fun createLangViews() {
        val languages =
            ArrayList(Arrays.asList(*resources.getStringArray(R.array.ra_lang_codes)))
        languages.add(0, null)

        val prefferedLang = LocaleManager.getInstance().languageSetting

        for (language in languages) {
            val item = getLanguageItem(language, TextUtils.equals(prefferedLang, language))
            item.setOnClickListener(this)
            LanguageList!!.addView(item)
        }
    }

    private fun getLanguageItem(language: String?, selected: Boolean): View {
        val inflater = LayoutInflater.from(requireContext())
        @SuppressLint("InflateParams") val item: FrameLayout =
            inflater.inflate(R.layout.language_item_layout, null) as FrameLayout
        val langName: TextView = item.findViewById(R.id.lang)
        val langInfo: TextView = item.findViewById(R.id.lang_info)
        val imageView: ImageView = item.findViewById(R.id.language_check)
        item.setTag(language)
        if (language == null) {
            langName.setText(R.string.settings_lang_select_default)
            langInfo.setText(R.string.settings_lang_select_expl_default)
        } else {
            val locale = Locale(language)
            langName.setText(StringUtils.capitalize(locale.displayName, locale))
            langInfo.setText(StringUtils.capitalize(locale.getDisplayName(locale), locale))
        }
        imageView.setVisibility(if (selected) View.VISIBLE else View.GONE)
        return item
    }

    override fun onClick(v: View) {
        setAppLanguage(v.tag as String?)
    }

    fun setAppLanguage(language: String?) {
        val locale = if (language != null) Locale(language) else null
        LocaleManager.getInstance().setLocale(locale)
        MyApplication.bus().post(LocaleChangedEvent(locale))
        //recreate()
    }
}