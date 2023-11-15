package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.*
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.R.*
import rs.readahead.washington.mobile.databinding.FragmentMainSettingsBinding
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class MainSettings : BaseBindingFragment<FragmentMainSettingsBinding>(
    FragmentMainSettingsBinding::inflate
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {

        binding.generalSettingsButton.setOnClickListener {
            nav().navigate(R.id.action_main_to_general_settings)
        }

        binding.securitySettingsButton.setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_security_settings)
        }

        binding.serversSettingsButton.setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_servers_settings)
        }

        binding.aboutNHelpSettingsButton.setOnClickListener {
            nav().navigate(R.id.action_main_settings_to_about_n_help_settings)
        }

        binding.toolbar.backClickListener = { back() }

        setUpToolbar()
    }


    private fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(binding.toolbar)
    }

}
