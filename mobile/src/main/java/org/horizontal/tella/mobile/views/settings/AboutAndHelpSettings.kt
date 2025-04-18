package org.horizontal.tella.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.horizontal.tella.mobile.BuildConfig
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentAboutNHelpSettingsBinding
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.views.base_ui.BaseFragment


class AboutAndHelpSettings : BaseFragment() {

    private var binding: FragmentAboutNHelpSettingsBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAboutNHelpSettingsBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun initView(view: View) {
        (baseActivity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_about_app_bar)

        binding?.version?.setText(
            String.format(
                "%s %s",
                getString(R.string.settings_about_app_version),
                BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
            )
        )
        binding?.tutorial?.setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_tutorial_url))
        }

        binding?.faq?.setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_faq_url))
        }

        binding?.contactUs?.setOnClickListener {
            Util.startSendMailIntent(context, getString(R.string.config_contact_url))
        }

        binding?.privacyPolicy?.setOnClickListener {
            Util.startBrowserIntent(context, getString(R.string.config_privacy_url))
        }
    }
}