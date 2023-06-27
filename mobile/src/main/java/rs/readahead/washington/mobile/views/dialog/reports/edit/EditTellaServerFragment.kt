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
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.SharedLiveData
import rs.readahead.washington.mobile.views.dialog.reports.ReportsConnectFlowViewModel
import rs.readahead.washington.mobile.views.dialog.reports.step6.SuccessfulSetServerFragment

internal const val EDIT_MODE_KEY = "edit_mode_key"

@AndroidEntryPoint
class EditTellaServerFragment :
    BaseBindingFragment<FragmentEditServerBinding>(FragmentEditServerBinding::inflate) {

    private val reportServer: TellaReportServer by lazy {
        Gson().fromJson(requireArguments().getString(OBJECT_KEY), TellaReportServer::class.java)
    }
    private val isEditMode: Boolean by lazy {
        requireArguments().getBoolean(EDIT_MODE_KEY)
    }
    private val viewModel: ReportsConnectFlowViewModel by viewModels()

    companion object {
        const val TAG = "EditTellaServerFragment"

        @JvmStatic
        fun newInstance(server: TellaReportServer, isEditMode: Boolean = false): EditTellaServerFragment {
            val editTellaServerFragment = EditTellaServerFragment()
            val args = Bundle().apply {
                putString(OBJECT_KEY, Gson().toJson(server))
                putBoolean(EDIT_MODE_KEY, isEditMode)
            }
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
            binding?.autoReportSwitch?.apply {
                mSwitch.isClickable = !(isAutoUploadActivated && !reportServer.isAutoUpload)
                val text = if (isAutoUploadActivated && !reportServer.isAutoUpload) {
                    R.string.Setting_Reports_Background_Upload_Disabled_Description
                } else {
                    R.string.Setting_Reports_Auto_Report_Description
                }
                setExplainText(text)
            }

            if (isAutoUploadActivated && !reportServer.isAutoUpload) {
                binding?.autoDeleteSeparator?.isVisible = false
                binding?.autoDeleteSwitch?.isVisible = false
            } else {
                val isVisible = isAutoUploadActivated || reportServer.isAutoUpload
                binding?.autoDeleteSeparator?.isVisible = isVisible
                binding?.autoDeleteSwitch?.isVisible = isVisible
            }
        })
    }


    private fun initView() {
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
        if (isEditMode) {
            binding?.credentialsContainer?.show()
        }
    }

    private fun initListeners() {
        binding?.apply {
            cancel.setOnClickListener { baseActivity.finish() }
            next.setOnClickListener {
                if (isEditMode) {
                    SharedLiveData.updateReportsServer.postValue(reportServer)
                    baseActivity.finish()
                } else {
                    baseActivity.addFragment(
                        SuccessfulSetServerFragment.newInstance(copyFields(reportServer)),
                        R.id.container
                    )
                }

            }

            autoDeleteSwitch.mSwitch.setOnCheckedChangeListener { _, isChecked ->
                reportServer.isAutoDelete = isChecked
            }

            autoReportSwitch.mSwitch.setOnCheckedChangeListener { _, isChecked ->
                reportServer.isAutoUpload = isChecked
                autoDeleteSeparator.isVisible = isChecked
                autoDeleteSwitch.apply {
                    isVisible = isChecked
                    if (!isChecked) mSwitch.isChecked = false
                }
            }

            backgroundUploadSwitch.mSwitch.setOnCheckedChangeListener { _, isChecked ->
                reportServer.isActivatedBackgroundUpload = isChecked
            }

            shareVerificationSwitch.mSwitch.setOnCheckedChangeListener { _, isChecked ->
                reportServer.isActivatedMetadata = isChecked
            }
        }
    }

    private fun copyFields(server: TellaReportServer): TellaReportServer {
        server.url = reportServer.url
        server.username = reportServer.username
        server.password = reportServer.password
        server.name = reportServer.name
        server.isActivatedMetadata = reportServer.isActivatedMetadata
        server.isActivatedBackgroundUpload = reportServer.isActivatedBackgroundUpload
        server.isAutoUpload = reportServer.isAutoUpload
        server.isAutoDelete = reportServer.isAutoDelete
        return server
    }

}