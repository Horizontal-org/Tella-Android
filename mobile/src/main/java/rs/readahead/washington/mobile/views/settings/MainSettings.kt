package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import org.hzontal.shared_ui.switches.TellaSwitchWithMessage
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.R.*
import rs.readahead.washington.mobile.data.sharedpref.Preferences


class MainSettings : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(layout.fragment_main_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(string.settings_app_bar)

        val offlineSwitch = view.findViewById<TellaSwitchWithMessage>(R.id.offline_switch)
        offlineSwitch.mSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            Preferences.setOfflineMode(isChecked)
        }
        offlineSwitch.setChecked(Preferences.isOfflineMode())

        view.findViewById<View>(R.id.general_settings_button).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_main_to_general_settings)
        }

        view.findViewById<View>(R.id.security_settings_button).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_main_settings_to_security_settings)
        }

        view.findViewById<View>(R.id.servers_settings_button).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_main_settings_to_servers_settings)
        }

        view.findViewById<View>(R.id.about_n_help_settings_button).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_main_settings_to_about_n_help_settings)
        }
        return view
    }
}
