package rs.readahead.washington.mobile.views.dialog.reports.step3

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentLoginReportsScreenBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.util.KeyboardLiveData
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.reports.ReportsConnectFlowViewModel
import rs.readahead.washington.mobile.views.dialog.reports.step4.SuccessfulLoginFragment

@AndroidEntryPoint
class LoginReportsFragment :
    BaseBindingFragment<FragmentLoginReportsScreenBinding>(FragmentLoginReportsScreenBinding::inflate) {
    private var validated = true
    private lateinit var serverReports: TellaReportServer
    private val viewModel by viewModels<ReportsConnectFlowViewModel>()

    companion object {
        @JvmStatic
        fun newInstance(server: TellaReportServer): LoginReportsFragment {
            val frag = LoginReportsFragment()
            val args = Bundle()
            args.putString(OBJECT_KEY, Gson().toJson(server))
            frag.arguments = args
            return frag
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
        initObservers()

    }

    private fun initListeners() {
        binding?.loginButton?.setOnClickListener {
            if (!MyApplication.isConnectedToInternet(baseActivity)) {
                DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(R.string.settings_docu_error_no_internet),
                    true
                )
            } else {
                validate()
                if (validated) {
                    viewModel.checkServer(copyFields(TellaReportServer(0)))
                }
            }
        }
    }

    private fun initObservers() {

        viewModel.error.observe(baseActivity, {
            binding?.passwordLayout?.error =
                getString(R.string.settings_docu_error_wrong_credentials)
        })

        viewModel.authenticationSuccess.observe(baseActivity, { isSuccess ->

            if (isSuccess) {
                baseActivity.addFragment(
                    SuccessfulLoginFragment.newInstance(
                        serverReports
                    ), R.id.container
                )
            }

        })

        viewModel.progress.observe(baseActivity, {
            binding?.progressBar?.isVisible = it
        })
    }

    private fun validate() {
        validated = true
        validateRequired(binding?.username, binding?.usernameLayout)
        validateRequired(binding?.password, binding?.passwordLayout)
    }

    private fun validateRequired(field: EditText?, layout: TextInputLayout?) {
        layout!!.error = null
        if (TextUtils.isEmpty(field!!.text.toString())) {
            layout.error = getString(R.string.settings_text_empty_field)
            validated = false
        }
    }

    private fun copyFields(server: TellaReportServer): TellaReportServer {
        server.url = serverReports.url
        server.username = binding?.username?.text.toString().trim(' ')
        server.password = binding?.password?.text.toString()
        server.name = serverReports.name
        serverReports = server
        return server
    }

    private fun initView() {
        arguments?.getString(OBJECT_KEY)?.let {
            serverReports = Gson().fromJson(it, TellaReportServer::class.java)
        }

        if (!serverReports.username.isNullOrEmpty() && !serverReports.password.isNullOrEmpty()) {
            binding?.username?.setText(serverReports.username)
            binding?.password?.setText(serverReports.password)
        }
        KeyboardLiveData(binding!!.root).observe(baseActivity, {
            binding?.backBtn?.isVisible = !it.first
        })
    }
}