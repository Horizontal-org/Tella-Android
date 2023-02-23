package rs.readahead.washington.mobile.views.dialog.reports.edit

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import rs.readahead.washington.mobile.databinding.FragmentEditServerBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.SharedLiveData

class EditTellaServerFragment :
    BaseBindingFragment<FragmentEditServerBinding>(FragmentEditServerBinding::inflate) {

    private lateinit var serverReports: TellaReportServer

    companion object {
        val TAG = EditTellaServerFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(server: TellaReportServer): EditTellaServerFragment {
            val frag = EditTellaServerFragment()
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
    }

    private fun initView() {
        arguments?.getString(OBJECT_KEY)?.let {
            serverReports = Gson().fromJson(it, TellaReportServer::class.java)
        }
        binding?.serverNameTv?.text = serverReports.name
        binding?.serverUrlTv?.text = serverReports.url
        binding?.userNameTv?.text = serverReports.username
        binding?.backgroundUploadSwitch?.mSwitch?.isChecked = serverReports.isActivatedBackgroundUpload
        binding?.shareVerificationSwitch?.mSwitch?.isChecked = serverReports.isActivatedMetadata
    }

    private fun initListeners() {
        binding?.cancel?.setOnClickListener {
            baseActivity.finish()
        }
        binding?.next?.setOnClickListener {
            SharedLiveData.updateReportsServer.postValue(serverReports)
            baseActivity.finish()
        }

        binding?.backgroundUploadSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked: Boolean ->
            serverReports.isActivatedBackgroundUpload = isChecked
        }

        binding?.shareVerificationSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked: Boolean ->
            serverReports.isActivatedMetadata = isChecked
        }

    }
}