package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import android.os.Bundle
import android.view.View
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentSendReportBinding
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments.BUNDLE_IS_FROM_OUTBOX
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.ReportsFormEndView
import org.horizontal.tella.mobile.views.fragment.vault.attachements.OnNavBckListener

abstract class BaseReportsSendFragment :
    BaseBindingFragment<FragmentSendReportBinding>(FragmentSendReportBinding::inflate),
    OnNavBckListener {

    abstract val viewModel: BaseReportsViewModel
    protected lateinit var endView: ReportsFormEndView
    protected var reportInstance: ReportInstance? = null
    protected var isFromOutbox = false
    protected var isFromDraft = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initData() {
        checkAndSubmitEntity(MyApplication.isConnectedToInternet(baseActivity))

        with(viewModel) {
            baseActivity.divviupUtils.runReportSentEvent()
            reportInstance.observe(viewLifecycleOwner) { instance ->
                when (instance.status) {
                    EntityStatus.SUBMITTED -> {
                        handleBackButton()
                        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
                    }

                    EntityStatus.SUBMISSION_PARTIAL_PARTS, EntityStatus.SUBMISSION_PENDING -> {
                        SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
                        handleBackButton()
                    }

                    else -> {

                    }
                }
            }
        }
    }

    // This method will be used to define the specific behavior for the back button in subclasses
    protected abstract fun navigateBack()

    protected fun handleBackButton() {
        navigateBack()
        reportInstance?.let { viewModel.submitReport(instance = it, true) }
        if (reportInstance?.status != EntityStatus.SUBMITTED) {
            DialogUtils.showBottomMessage(
                baseActivity, getString(R.string.Report_Available_in_Outbox), false
            )
        } else {
            DialogUtils.showBottomMessage(
                baseActivity, getString(R.string.report_submitted_msg), false
            )
        }
    }

    private fun initView() {
        arguments?.let { bundle ->
            reportInstance = bundle.getSerializable(BUNDLE_REPORT_FORM_INSTANCE) as ReportInstance
            isFromOutbox = bundle.getBoolean(BUNDLE_IS_FROM_OUTBOX)
            isFromDraft = bundle.getBoolean(BUNDLE_IS_FROM_DRAFT)
            showFormEndView()
        }

        binding.toolbar.backClickListener = {
            handleBackButton()
        }
        binding.setRightIcon(icon = -1)

        if (reportInstance?.status == EntityStatus.SUBMITTED) {
            binding.nextBtn.hide()
        }
        highlightSubmitButton()
    }

    private fun checkAndSubmitEntity(isOnline: Boolean) {
        if (!isOnline) {
            binding.nextBtn.text = getString(R.string.Reports_Resume)
            return
        } else {
            if (isFromDraft) {
                submitEntity()
            } else {
                pauseResumeLabel(reportInstance)
            }
        }
    }

    private fun highlightSubmitButton() {
        binding.nextBtn.setOnClickListener {
            if (reportInstance?.status == EntityStatus.SUBMISSION_IN_PROGRESS) {
                viewModel.clearDisposable()
            } else {
                submitEntity()
            }
        }
        pauseResumeLabel(reportInstance)
    }

    protected fun pauseResumeLabel(reportFormInstance: ReportInstance?) {
        if (reportFormInstance?.status == EntityStatus.SUBMISSION_IN_PROGRESS || reportFormInstance?.status == EntityStatus.SUBMISSION_IN_PROGRESS) {
            binding.nextBtn.text = getString(R.string.Reports_Pause)
        } else {
            binding.nextBtn.text = getString(R.string.Reports_Resume)
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
                reportFormInstance, MyApplication.isConnectedToInternet(baseActivity), false
            )
            binding.endViewContainer.removeAllViews()
            binding.endViewContainer.addView(endView)
            endView.clearPartsProgress(reportFormInstance)
        }
    }

    private fun submitEntity() {
        reportInstance?.let { entity ->
            viewModel.submitReport(entity, false)
        }
    }

    override fun onBackPressed(): Boolean {
        handleBackButton()
        return true
    }
}
