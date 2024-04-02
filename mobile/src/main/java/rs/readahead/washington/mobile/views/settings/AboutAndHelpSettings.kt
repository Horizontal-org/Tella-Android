package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import rs.readahead.washington.mobile.BuildConfig
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentAboutNHelpSettingsBinding
import rs.readahead.washington.mobile.util.Util
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment


class AboutAndHelpSettings : BaseBindingFragment<FragmentAboutNHelpSettingsBinding>(FragmentAboutNHelpSettingsBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {

        binding.version?.text = String.format(
            "%s %s",
            getString(R.string.settings_about_app_version),
            BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
        )

        binding.faq.setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_faq_url))
        }

        binding.contactUs.setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_contact_url))
        }

        binding.privacyPolicy.setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_privacy_url))
        }

        handleOnBackPressed()
    }

    private fun handleOnBackPressed() {
        binding.toolbar.backClickListener = {
            nav().popBackStack()
        }
        (activity as MainActivity).onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    nav().popBackStack()
                }
            })
    }
}