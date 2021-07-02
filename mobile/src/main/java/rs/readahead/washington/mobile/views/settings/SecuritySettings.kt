package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import rs.readahead.washington.mobile.R


class SecuritySettings : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_security_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_sec_app_bar)

        return view
    }
}