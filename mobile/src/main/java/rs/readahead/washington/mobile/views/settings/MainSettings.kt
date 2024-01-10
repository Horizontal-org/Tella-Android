package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.View
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.R.string
import rs.readahead.washington.mobile.databinding.FragmentMainSettingsBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment

class MainSettings :
    BaseBindingFragment<FragmentMainSettingsBinding>(FragmentMainSettingsBinding::inflate),
    View.OnClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    fun initView() {
        (baseActivity as OnFragmentSelected?)?.setToolbarLabel(string.settings_app_bar)
        initListeners()
    }

    private fun initListeners() {
        binding.generalSettingsButton.setOnClickListener(this)
        binding.securitySettingsButton.setOnClickListener(this)
        binding.serversSettingsButton.setOnClickListener(this)
        binding.aboutNHelpSettingsButton.setOnClickListener(this)
        binding.feedbackButton.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.general_settings_button -> {
                nav().navigate(R.id.action_main_to_general_settings)
            }

            R.id.security_settings_button -> {
                nav().navigate(R.id.action_main_settings_to_security_settings)
            }

            R.id.servers_settings_button -> {
                nav().navigate(R.id.action_main_settings_to_servers_settings)
            }

            R.id.about_n_help_settings_button -> {
                nav().navigate(R.id.action_main_settings_to_about_n_help_settings)
            }

            R.id.feedback_button -> {
                nav().navigate(R.id.action_main_settings_to_sendFeedbackFragment)
            }
        }

        binding.toolbar.backClickListener = { back() }

    }
}
