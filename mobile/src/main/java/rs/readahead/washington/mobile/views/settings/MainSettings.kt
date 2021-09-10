package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import android.widget.ImageView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.R.*
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseFragment


class MainSettings : BaseFragment() {

    private val cm = CamouflageManager.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(layout.fragment_main_settings, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.setToolbarLabel(string.settings_app_bar)

        val offlineSwitch = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.offline_switch)
        offlineSwitch.setOnCheckedChangeListener { switch: CompoundButton?, isChecked: Boolean ->
            Preferences.setOfflineMode(isChecked)
        }
        offlineSwitch.setChecked(Preferences.isOfflineMode())

        view.findViewById<View>(R.id.general_settings_button).setOnClickListener {
            nav().navigate(R.id.action_main_to_general_settings)
        }

        view.findViewById<View>(R.id.security_settings_button).setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_security_settings)
        }

        view.findViewById<View>(R.id.servers_settings_button).setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_servers_settings)
        }

        view.findViewById<View>(R.id.about_n_help_settings_button).setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_about_n_help_settings)
        }

        val offlineTooltip = view.findViewById<ImageView>(R.id.offline_tooltip)
        offlineTooltip.setOnClickListener({
            showTooltip(
                    offlineTooltip,
                    resources.getString(R.string.offline_dialog_expl),
                    Gravity.BOTTOM
            )
        })
    }

}
