package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import rs.readahead.washington.mobile.BuildConfig
import rs.readahead.washington.mobile.R


class AboutAndHelpSettings : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about_n_help_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_about_app_bar)

        val version = view.findViewById<TextView>(R.id.version)
        version.setText(
            String.format(
                "%s %s",
                getString(R.string.settings_about_app_version),
                BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
            )
        )
        return view
    }
}