package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.BuildConfig
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.Util
import rs.readahead.washington.mobile.views.base_ui.BaseFragment


class AboutAndHelpSettings : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about_n_help_settings, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_about_app_bar)

        val version = view.findViewById<TextView>(R.id.version)
        version.setText(
            String.format(
                "%s %s",
                getString(R.string.settings_about_app_version),
                BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
            )
        )

        view.findViewById<View>(R.id.faq).setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_faq_url))
        }

        view.findViewById<View>(R.id.contact_us).setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_contact_url))
        }

        view.findViewById<View>(R.id.privacy_policy).setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_privacy_url))
        }
    }
}