package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import rs.readahead.washington.mobile.R


class AboutAndHelpSettings : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about_n_help_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_about_app_bar)

        return view
    }
}