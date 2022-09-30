package rs.readahead.washington.mobile.views.dialog.reports.step1

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentEnterServerBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.ConnectFlowUtils.validateUrl
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.TITLE_KEY
import rs.readahead.washington.mobile.views.dialog.reports.step3.LoginReportsFragment

@AndroidEntryPoint
class EnterUploadServerFragment :
    BaseBindingFragment<FragmentEnterServerBinding>(FragmentEnterServerBinding::inflate) {
    private val server by lazy { TellaReportServer() }
    private var serverReports: TellaReportServer? = null
    private var isUpdate = false

    companion object {
        val TAG = EnterUploadServerFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(server: TellaReportServer, isUpdate: Boolean): EnterUploadServerFragment {
            val frag = EnterUploadServerFragment()
            val args = Bundle()
            args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
            args.putSerializable(ID_KEY, server.id)
            args.putString(OBJECT_KEY, Gson().toJson(server))
            args.putBoolean(IS_UPDATE_SERVER, isUpdate)
            frag.arguments = args
            return frag
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
    }

    private fun initListeners() {
        with(binding!!) {
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
                    if (validateUrl(url, urlLayout, baseActivity, server)) {
                        KeyboardUtil.hideKeyboard(activity)
                        baseActivity.addFragment(
                            LoginReportsFragment.newInstance(
                                server,
                                isUpdate
                            ), R.id.container
                        )
                    }
                }
            }
        }
    }

    private fun initView() {
        if (serverReports != null) {
            binding?.url?.setText(serverReports!!.url)
        }
        if (arguments == null) return

        arguments?.getString(OBJECT_KEY)?.let {
            serverReports = Gson().fromJson(it, TellaReportServer::class.java)
        }
        arguments?.getBoolean(IS_UPDATE_SERVER)?.let {
            isUpdate = it
        }
    }

}