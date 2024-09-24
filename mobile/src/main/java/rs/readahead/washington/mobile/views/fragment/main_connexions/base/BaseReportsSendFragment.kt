package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import android.os.Bundle
import android.view.View
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSendReportBinding
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments.BUNDLE_IS_FROM_OUTBOX
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.ReportsFormEndView
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener

abstract class BaseReportsSendFragment :
    BaseBindingFragment<FragmentSendReportBinding>(FragmentSendReportBinding::inflate),
    OnNavBckListener {

    abstract val viewModel: BaseReportsViewModel
    private lateinit var endView: ReportsFormEndView
    private var reportInstance: ReportInstance? = null
    protected var isFromOutbox = false
    private var isFromDraft = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initData() {
        checkAndSubmitEntity(MyApplication.isConnectedToInternet(baseActivity))

        with(viewModel) {
            reportProcess.observe(viewLifecycleOwner) { progress ->
                if (progress.second.id == this@BaseReportsSendFragment.reportInstance?.id) {
                    val pct = progress.first
                    val instance = progress.second

                    pauseResumeLabel(instance)
                    endView.setUploadProgress(instance, pct.current.toFloat() / pct.size.toFloat())
                }
            }

            instanceProgress.observe(viewLifecycleOwner) { entity ->
                if (entity.id == this@BaseReportsSendFragment.reportInstance?.id) {
                    when (entity.status) {
                        EntityStatus.SUBMITTED -> {
                            viewModel.saveSubmitted(entity)
                        }

                        EntityStatus.FINALIZED -> {
                            viewModel.saveOutbox(entity)
                        }

                        EntityStatus.PAUSED -> {
                            pauseResumeLabel(entity)
                            viewModel.saveOutbox(entity)
                        }

                        EntityStatus.DELETED -> {
                            instanceProgress.postValue(null)
                            handleBackButton()
                        }

                        else -> {
                            this@BaseReportsSendFragment.reportInstance = entity
                        }
                    }
                }
            }

            reportInstance.observe(viewLifecycleOwner) { instance ->
                when (instance.status) {
                    EntityStatus.SUBMITTED -> {
                        handleBackButton()
                        SharedLiveData.updateViewPagerPosition.postValue(SUBMITTED_LIST_PAGE_INDEX)
                    }

                    EntityStatus.SUBMISSION_PARTIAL_PARTS, EntityStatus.SUBMISSION_PENDING -> {
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

    private fun handleBackButton() {
        navigateBack()
        reportInstance?.let { viewModel.submitReport(instance = it, true) }
        DialogUtils.showBottomMessage(
            baseActivity, getString(R.string.Report_Available_in_Outbox), false
        )
    }

    private fun initView() {
        arguments?.let { bundle ->
            reportInstance = bundle.get(BUNDLE_REPORT_FORM_INSTANCE) as ReportInstance
            isFromOutbox = bundle.getBoolean(BUNDLE_IS_FROM_OUTBOX)
            isFromDraft = bundle.getBoolean(BUNDLE_IS_FROM_DRAFT)
            showFormEndView()
        }

        binding.toolbar.backClickListener = {
            handleBackButton()
        }
        binding.toolbar.setRightIcon(icon = -1)

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

    private fun pauseResumeLabel(reportFormInstance: ReportInstance?) {
        if (reportFormInstance?.status == EntityStatus.SUBMISSION_IN_PROGRESS) {
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
