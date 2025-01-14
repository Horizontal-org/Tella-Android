package org.horizontal.tella.mobile.views.dialog.reports.step1

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentEnterServerBinding
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.dialog.ConnectFlowUtils.validateUrl
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.reports.ReportsConnectFlowViewModel
import org.horizontal.tella.mobile.views.dialog.reports.step3.OBJECT_SLUG

@AndroidEntryPoint
class EnterUploadServerFragment : BaseBindingFragment<FragmentEnterServerBinding>(
    FragmentEnterServerBinding::inflate
) {
    private val viewModel: ReportsConnectFlowViewModel by viewModels()
    private val serverReports: TellaReportServer by lazy {
        TellaReportServer()
    }
    private var projectSlug = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initView()
    }

    fun initView() {
        with(binding) {
            backBtn.setOnClickListener {
                baseActivity.finish()
            }
            nextBtn.setOnClickListener {
                if (!MyApplication.isConnectedToInternet(baseActivity)) {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        getString(R.string.settings_docu_error_no_internet),
                        true
                    )
                } else {
                    if (validateUrl(url, urlLayout, baseActivity, serverReports)) {
                        val url = serverReports.url
                        projectSlug = url.substring(url.lastIndexOf('/') - 1)
                        serverReports.url = url.substring(0, url.lastIndexOf('/') - 1)
                        viewModel.listServers(url)
                    }
                }
            }
        }
    }

    private fun initObservers() {
        viewModel.serverAlreadyExist.observe(baseActivity) { serverAlreadyExist ->
            if (serverAlreadyExist) {
                DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(R.string.Report_Server_Already_Exist_Error),
                    false
                )
            } else {
                KeyboardUtil.hideKeyboard(baseActivity, view)
                bundle.putString(OBJECT_KEY, Gson().toJson(serverReports))
                bundle.putString(OBJECT_SLUG, projectSlug)
                navManager().navigateFromEnterUploadServerFragmentToLoginReportsFragment()
            }
        }
    }

}