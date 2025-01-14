package org.horizontal.tella.mobile.views.dialog.reports.step4

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentSuccessfulLoginBinding
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.SharedLiveData

class SuccessfulLoginFragment : BaseBindingFragment<FragmentSuccessfulLoginBinding>(
    FragmentSuccessfulLoginBinding::inflate
) {
    private lateinit var server: TellaReportServer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
    }

    private fun initView() {
        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, TellaReportServer::class.java)
        }
    }

    private fun initListeners() {
        with(binding) {
            goToAdvancedSettingsBtn.setOnClickListener {
                goToAdvancedSettingsBtn.isChecked = true
                goToReportsBtn.isChecked = false
                bundle.putString(OBJECT_KEY, Gson().toJson(server))
                navManager().navigateFromSuccessfulLoginFragmentToServerAdvancedSettingsFragment()
            }
            goToReportsBtn.setOnClickListener {
                goToAdvancedSettingsBtn.isChecked = false
                goToReportsBtn.isChecked = true
                saveServerAndGoToReportsScreen()
            }
        }
    }

    private fun saveServerAndGoToReportsScreen() {
        SharedLiveData.createReportsServerAndCloseActivity.postValue(server)
        navManager().navigateTo(R.id.reports_graph)
        baseActivity.finish()
    }

}