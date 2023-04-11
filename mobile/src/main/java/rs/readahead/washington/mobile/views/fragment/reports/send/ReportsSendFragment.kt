package rs.readahead.washington.mobile.views.fragment.reports.send

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentSendReportBinding
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.util.ConnectionLiveData
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.fragment.reports.ReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.entry.BUNDLE_REPORT_FORM_INSTANCE
import rs.readahead.washington.mobile.views.fragment.reports.viewpager.SUBMITTED_LIST_PAGE_INDEX
import rs.readahead.washington.mobile.views.fragment.reports.viewpagerfragments.BUNDLE_IS_FROM_OUTBOX
import rs.readahead.washington.mobile.views.fragment.uwazi.SharedLiveData
import rs.readahead.washington.mobile.views.fragment.uwazi.widgets.ReportsFormEndView

@AndroidEntryPoint
class ReportsSendFragment :
    BaseBindingFragment<FragmentSendReportBinding>(FragmentSendReportBinding::inflate) {

    private val viewModel by viewModels<ReportsViewModel>()
    private lateinit var endView: ReportsFormEndView
    private var reportInstance: ReportInstance? = null
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
            reportProcess.observe(viewLifecycleOwner) { progress ->
                if (progress.second.id == this@ReportsSendFragment.reportInstance?.id) {
                    val pct = progress.first
                    val instance = progress.second

                    pauseResumeLabel(instance)
                    endView.setUploadProgress(instance, pct.current.toFloat() / pct.size.toFloat())
                }
            }

            instanceProgress.observe(viewLifecycleOwner) { entity ->
                if (entity == null) {
                    return@observe
                }
                if (entity.id == this@ReportsSendFragment.reportInstance?.id) {
                    when (entity.status) {
                        EntityStatus.SUBMITTED -> {
                            viewModel.saveSubmitted(entity)
                            instanceProgress.postValue(null)
                        }
                        EntityStatus.SUBMISSION_ERROR, EntityStatus.FINALIZED -> {
                            viewModel.saveOutbox(entity)
                        }
                        EntityStatus.PAUSED -> {
                            pauseResumeLabel(entity)
                            viewModel.saveOutbox(entity)
                        }
                        else -> {

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
        arguments?.let { bundle ->
            reportInstance = bundle.get(BUNDLE_REPORT_FORM_INSTANCE) as ReportInstance
            isFromOutbox = bundle.getBoolean(BUNDLE_IS_FROM_OUTBOX)
            showFormEndView()
        }

        binding?.toolbar?.backClickListener = {
            handleBackButton()
        }
        binding?.toolbar?.setRightIcon(-1)

        if (reportInstance?.status == EntityStatus.SUBMITTED) {
            binding?.nextBtn?.hide()
        }
        highlightSubmitButton()
        handleOnBackPressed()
    }

    private fun checkAndSubmitEntity(isOnline: Boolean) {
        if (!isOnline) {
            binding?.nextBtn?.text = getString(R.string.Reports_Resume)
        } else {
            pauseResumeLabel(reportInstance)
        }

    }

    private fun highlightSubmitButton() {
        binding?.nextBtn?.setOnClickListener {
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
            binding?.nextBtn?.text = getString(R.string.Reports_Pause)
        } else {
            binding?.nextBtn?.text = getString(R.string.Reports_Resume)
        }
    }

    private fun handleOnBackPressed() {
        (activity as MainActivity).onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    viewModel.clearDisposable()
                }
            })
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