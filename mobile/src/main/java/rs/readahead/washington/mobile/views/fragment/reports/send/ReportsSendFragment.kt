package rs.readahead.washington.mobile.views.fragment.reports.send

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.databinding.FragmentSendReportBinding
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.reports.entry.ReportsEntryViewModel
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.ReportsFormEndView
import timber.log.Timber

@AndroidEntryPoint
class ReportsSendFragment :
    BaseBindingFragment<FragmentSendReportBinding>(FragmentSendReportBinding::inflate) {

    private val viewModel by viewModels<ReportsEntryViewModel>()
    private lateinit var endView: ReportsFormEndView
    private var reportInstance: ReportFormInstance? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initData() {
        with(viewModel) {
            progressInfo.observe(viewLifecycleOwner) {
                //endView.setUploadProgress(it.name,it.current.toFloat() / it.size.toFloat())
                endView.setUploadProgress(it.fileId, it.current.toFloat())
                if (it.status == UploadProgressInfo.Status.FINISHED){
                    endView.hideUploadProgress(it.fileId)
                }
            }

            entityStatus.observe(viewLifecycleOwner) { entity ->
                when (entity.status) {
                    EntityStatus.SUBMITTED -> {
                        viewModel.saveSubmitted(entity)
                    }
                    EntityStatus.SUBMISSION_ERROR, EntityStatus.SUBMISSION_PARTIAL_PARTS, EntityStatus.SUBMISSION_PENDING, EntityStatus.PAUSED, EntityStatus.FINALIZED -> {
                        viewModel.saveOutbox(entity)
                    }
                    else -> {

                    }
                }
            }

            reportInstance.observe(viewLifecycleOwner) { instance ->
                when (instance.status) {
                    EntityStatus.SUBMITTED -> {
                        nav().popBackStack()
                        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
                    }
                    EntityStatus.SUBMISSION_ERROR, EntityStatus.SUBMISSION_PARTIAL_PARTS, EntityStatus.SUBMISSION_PENDING -> {
                        nav().popBackStack()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun initView() {
        arguments?.let {
            reportInstance = it.get(BUNDLE_REPORT_FORM_INSTANCE) as ReportFormInstance
            showFormEndView()
        }

        with(binding) {
            this?.toolbar?.backClickListener = { nav().popBackStack() }

            this?.nextBtn?.setOnClickListener {
                reportInstance?.let {
                    submitEntity()
                }
            }
        }
    }

    private fun showFormEndView() {
        if (reportInstance == null) {
            return
        }

        reportInstance?.let { reportFormInstance ->
            if (reportFormInstance.status == EntityStatus.SUBMITTED) {
                binding?.nextBtn?.hide()
            }
            endView = ReportsFormEndView(
                activity,
                reportFormInstance.title,
                reportFormInstance.description,
                getStatusLabel(reportFormInstance)
            )
            endView.setInstance(reportFormInstance, false, true)
            binding?.endViewContainer?.removeAllViews()
            binding?.endViewContainer?.addView(endView)
            endView.clearPartsProgress(reportFormInstance)
        }
    }

    fun submitEntity() {
        reportInstance?.let { entity ->
            viewModel.submitReport(entity)
        }
    }

    private fun getStatusLabel(reportFormInstance: ReportFormInstance): String {
        if (reportFormInstance.status == EntityStatus.SUBMITTED) {
            return ""
        } else {
            return ""
        }
    }
}