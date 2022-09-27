package rs.readahead.washington.mobile.views.dialog.reports.url

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentEnterServerBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.dialog.ConnectFlowUtils.validateUrl
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.TITLE_KEY

class EnterUploadServerFragment : BaseFragment() {
    private lateinit var binding: FragmentEnterServerBinding
    private var isUpdate = false
    private var server: TellaReportServer? = null

    companion object {
        val TAG = EnterUploadServerFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(server: TellaReportServer, isUpdate: Boolean): EnterUploadServerFragment {
            val frag = EnterUploadServerFragment()
            val args = Bundle()
            args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
            args.putSerializable(ID_KEY, server.id)
            args.putString(OBJECT_KEY, Gson().toJson(server))
            args.putBoolean(rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER, isUpdate)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEnterServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    override fun initView(view: View) {
        arguments?.getBoolean(rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER)?.let {
            isUpdate = it
        }

        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it, TellaReportServer::class.java)
        }
        if (server != null) {
            binding.url.setText(server!!.url)
        }
    }

    private fun initListeners() {
        with(binding) {
            backBtn.setOnClickListener {
                activity.finish()
            }
            nextBtn.setOnClickListener {
                if (!MyApplication.isConnectedToInternet(activity)) {
                    DialogUtils.showBottomMessage(
                        activity,
                        getString(R.string.settings_docu_error_no_internet),
                        true
                    )
                } else {

                    if (validateUrl(url, urlLayout, activity, server)) {

                    }
                }
            }
        }
    }

}