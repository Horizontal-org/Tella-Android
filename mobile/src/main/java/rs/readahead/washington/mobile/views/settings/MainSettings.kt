package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.*
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.R.*
import rs.readahead.washington.mobile.databinding.FragmentMainSettingsBinding
import rs.readahead.washington.mobile.views.base_ui.BaseFragment


class MainSettings : BaseFragment() {

    private var binding: FragmentMainSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainSettingsBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.setToolbarLabel(string.settings_app_bar)

        binding?.generalSettingsButton?.setOnClickListener {
            nav().navigate(R.id.action_main_to_general_settings)
        }

        binding?.securitySettingsButton?.setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_security_settings)
        }

        binding?.serversSettingsButton?.setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_servers_settings)
        }

        binding?.aboutNHelpSettingsButton?.setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_about_n_help_settings)
        }
    }

}
