package rs.readahead.washington.mobile.views.dialog.reports.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentEditServerBinding
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.SharedLiveData
import rs.readahead.washington.mobile.views.dialog.reports.ReportsConnectFlowViewModel

@AndroidEntryPoint
class EditTellaServerFragment :
    BaseBindingFragment<FragmentEditServerBinding>(FragmentEditServerBinding::inflate) {

    private lateinit var reportServer: TellaReportServer

    private val viewModel: ReportsConnectFlowViewModel by viewModels()


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
        initData()
        initListeners()
    }

    private fun initData() {
        viewModel.listAutoReports()
        viewModel.doesAutoUploadActivated.observe(viewLifecycleOwner, { isAutoUploadActivated ->
            if (isAutoUploadActivated && !reportServer.isAutoUpload) {
                binding?.autoReportSwitch?.mSwitch?.isClickable = false
                binding?.autoReportSwitch?.setExplainText(R.string.Setting_Reports_Background_Upload_Disabled_Description)

            } else {
                binding?.autoReportSwitch?.mSwitch?.isClickable = true
                binding?.autoReportSwitch?.setExplainText(R.string.Setting_Reports_Background_Upload_Description)
            }
        })
    }

    private fun initView() {
        arguments?.getString(OBJECT_KEY)?.let {
            reportServer = Gson().fromJson(it, TellaReportServer::class.java)
        }
        reportServer.apply {
            binding?.serverNameTv?.text = name
            binding?.serverUrlTv?.text = url
            binding?.userNameTv?.text = username
            binding?.backgroundUploadSwitch?.mSwitch?.isChecked = isActivatedBackgroundUpload
            binding?.shareVerificationSwitch?.mSwitch?.isChecked = isActivatedMetadata
            binding?.autoReportSwitch?.mSwitch?.isChecked = isAutoUpload
            binding?.autoDeleteSwitch?.mSwitch?.isChecked = isAutoDelete
        }

    }

    private fun initListeners() {
        binding?.cancel?.setOnClickListener {
            baseActivity.finish()
        }
        binding?.next?.setOnClickListener {
            SharedLiveData.updateReportsServer.postValue(reportServer)
            baseActivity.finish()
        }

        binding?.autoDeleteSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked: Boolean ->
            reportServer.isAutoDelete = isChecked
        }

        binding?.autoReportSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked: Boolean ->
            reportServer.isAutoUpload = isChecked
        }

        binding?.backgroundUploadSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked: Boolean ->
            reportServer.isActivatedBackgroundUpload = isChecked
        }

        binding?.shareVerificationSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked: Boolean ->
            reportServer.isActivatedMetadata = isChecked
        }

    }
}