package rs.readahead.washington.mobile.views.dialog.reports.edit

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
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
        const val TAG = "EditTellaServerFragment"

        @JvmStatic
        fun newInstance(server: TellaReportServer): EditTellaServerFragment {
            val editTellaServerFragment = EditTellaServerFragment()
            val args = Bundle()
            args.putString(OBJECT_KEY, Gson().toJson(server))
            editTellaServerFragment.arguments = args
            return editTellaServerFragment
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

            binding?.autoDeleteSeparator?.isVisible = isAutoUploadActivated || reportServer.isAutoUpload
            binding?.autoDeleteSwitch?.isVisible = isAutoUploadActivated || reportServer.isAutoUpload

        })
    }

    private fun initView() {
        arguments?.getString(OBJECT_KEY)?.let {
            reportServer = Gson().fromJson(it, TellaReportServer::class.java)
        }
        reportServer.apply {
            binding?.apply {
                serverNameTv.text = name.orEmpty()
                serverUrlTv.text = url.orEmpty()
                userNameTv.text = username.orEmpty()
                backgroundUploadSwitch.mSwitch.isChecked = isActivatedBackgroundUpload
                shareVerificationSwitch.mSwitch.isChecked = isActivatedMetadata
                autoReportSwitch.mSwitch.isChecked = isAutoUpload
                autoDeleteSwitch.mSwitch.isChecked = isAutoDelete
            }
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

        binding?.autoDeleteSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked ->
            reportServer.isAutoDelete = isChecked
        }

        binding?.autoReportSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked ->
            reportServer.isAutoUpload = isChecked
            binding?.autoDeleteSeparator?.isVisible = isChecked
            binding?.autoDeleteSwitch?.isVisible = isChecked

        }

        binding?.backgroundUploadSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked ->
            reportServer.isActivatedBackgroundUpload = isChecked
        }

        binding?.shareVerificationSwitch?.mSwitch?.setOnCheckedChangeListener { _, isChecked ->
            reportServer.isActivatedMetadata = isChecked
        }

    }
}