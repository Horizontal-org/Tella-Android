package rs.readahead.washington.mobile.views.fragment.reports.send

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSendReportBinding
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.util.ConnectionLiveData
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments.BUNDLE_IS_FROM_OUTBOX
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.ReportsFormEndView

@AndroidEntryPoint
class ReportsSendFragment :
    BaseBindingFragment<FragmentSendReportBinding>(FragmentSendReportBinding::inflate) {

    private val viewModel by viewModels<ReportsViewModel>()
    private lateinit var endView: ReportsFormEndView
    private var reportInstance: ReportFormInstance? = null
    private var isFromOutbox = false
    private lateinit var connectionLiveData: ConnectionLiveData

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initData() {
        connectionLiveData = ConnectionLiveData(baseActivity)

        connectionLiveData.observe(viewLifecycleOwner) { isOnline ->
            checkAndSubmitEntity(isOnline)
        }

        with(viewModel) {
            progressInfo.observe(viewLifecycleOwner) { progress ->
                val pct = progress.first
                val instance = progress.second
                endView.setUploadProgress(instance, pct.current.toFloat() / pct.size.toFloat())
            }

            entityStatus.observe(viewLifecycleOwner) { entity ->
                when (entity.status) {
                    EntityStatus.SUBMITTED -> {
                        viewModel.saveSubmitted(entity)
                    }
                    EntityStatus.SUBMISSION_ERROR, EntityStatus.FINALIZED -> {
                        viewModel.saveOutbox(entity)
                    }
                    EntityStatus.SUBMISSION_PARTIAL_PARTS, EntityStatus.SUBMISSION_PENDING -> {
                        pauseResumeLabel()
                    }
                    EntityStatus.PAUSED -> {
                        viewModel.saveOutbox(entity)
                        pauseResumeLabel()
                    }
                    else -> {

                    }
                }
            }

            reportInstance.observe(viewLifecycleOwner) { instance ->
                when (instance.status) {
                    EntityStatus.SUBMITTED -> {
                        handleBackButton()
                        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
                    }
                    EntityStatus.SUBMISSION_ERROR, EntityStatus.SUBMISSION_PARTIAL_PARTS, EntityStatus.SUBMISSION_PENDING -> {
                        handleBackButton()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleBackButton() {
        if (isFromOutbox) {
            nav().popBackStack()
        } else {
            nav().popBackStack(R.id.newReportScreen, true)
        }
    }

    private fun initView() {
        arguments?.let {
            reportInstance = it.get(BUNDLE_REPORT_FORM_INSTANCE) as ReportFormInstance
            isFromOutbox = it.getBoolean(BUNDLE_IS_FROM_OUTBOX)
            showFormEndView()
        }
        if (isFromOutbox) {
            binding?.nextBtn?.hide()
        }
        binding?.toolbar?.backClickListener = {
            handleBackButton()
        }
        binding?.toolbar?.setRightIcon(-1)

        if (reportInstance?.status == EntityStatus.SUBMITTED) {
            binding?.nextBtn?.hide()
        }
        highlightSubmitButton()
    }

    private fun checkAndSubmitEntity(isOnline: Boolean) {
        if (!isOnline) {
            binding?.nextBtn?.text = getString(R.string.Reports_Resume)
        } else {
            if (isFromOutbox) {
                submitEntity()
            } else {
                if (reportInstance?.status == EntityStatus.PAUSED || reportInstance?.status == EntityStatus.SUBMISSION_ERROR) {
                    binding?.nextBtn?.text = getString(R.string.Reports_Resume)
                } else {
                    if (reportInstance?.status != EntityStatus.SUBMITTED) {
                        binding?.nextBtn?.text = getString(R.string.Reports_Pause)
                        submitEntity()
                    }
                }
            }
        }

    }

    private fun highlightSubmitButton() {
        binding?.nextBtn?.setOnClickListener {
            if (reportInstance?.status == EntityStatus.PAUSED || reportInstance?.status == EntityStatus.SUBMISSION_ERROR) {
                submitEntity()
            } else {
                viewModel.clearDisposabe()
            }
        }
        pauseResumeLabel()
    }

    private fun pauseResumeLabel() {
        if (reportInstance?.status == EntityStatus.PAUSED) {
            binding?.nextBtn?.text = getString(R.string.Reports_Resume)
        } else {
            binding?.nextBtn?.text = getString(R.string.Reports_Pause)
        }
    }

    private fun showFormEndView() {
        if (reportInstance == null) {
            return
        }

        reportInstance?.let { reportFormInstance ->

            endView = ReportsFormEndView(
                activity,
                reportFormInstance.title,
                reportFormInstance.description,
            )
            endView.setInstance(
                reportFormInstance,
                MyApplication.isConnectedToInternet(baseActivity),
                false
            )
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
}