package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import android.os.Bundle
import android.view.View
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentSendReportBinding
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.IEntityInstance
import org.horizontal.tella.mobile.util.hide
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.reports.viewpagerfragments.BUNDLE_IS_FROM_OUTBOX
import org.horizontal.tella.mobile.views.fragment.vault.attachements.OnNavBckListener

/**
 * Submission screen shared by every connection type. Subclasses supply how the instance is read
 * out of the arguments and how its end view is rendered, since a report end view lists a
 * description while a Uwazi end view lists the entity's template.
 */
abstract class BaseEntitySendFragment<I : IEntityInstance> :
    BaseBindingFragment<FragmentSendReportBinding>(FragmentSendReportBinding::inflate),
    OnNavBckListener {

    abstract val viewModel: BaseEntityListViewModel<I>
    protected var reportInstance: I? = null
    protected var isFromOutbox = false
    protected var isFromDraft = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    protected abstract fun readInstanceFromArguments(arguments: Bundle): I?

    protected abstract fun showFormEndView()

    // This method will be used to define the specific behavior for the back button in subclasses
    protected abstract fun navigateBack()

    protected open fun trackSentEvent() {
        baseActivity.divviupUtils.runReportSentEvent()
    }

    protected open fun getPendingMessage(): String = getString(R.string.Report_Available_in_Outbox)

    protected open fun getSubmittedMessage(): String = getString(R.string.report_submitted_msg)

    private fun initData() {
        checkAndSubmitEntity(MyApplication.isConnectedToInternet(baseActivity))

        with(viewModel) {
            trackSentEvent()
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

    protected fun handleBackButton() {
        navigateBack()
        reportInstance?.let {
            if (it.status != EntityStatus.PAUSED) {
                viewModel.submitReport(instance = it, true)
            }
        }
        if (reportInstance?.status != EntityStatus.SUBMITTED) {
            DialogUtils.showBottomMessage(baseActivity, getPendingMessage(), false)
        } else {
            DialogUtils.showBottomMessage(baseActivity, getSubmittedMessage(), false)
        }
    }

    private fun initView() {
        arguments?.let { bundle ->
            reportInstance = readInstanceFromArguments(bundle)
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

    protected fun pauseResumeLabel(reportFormInstance: I?) {
        if (reportFormInstance?.status == EntityStatus.SUBMISSION_IN_PROGRESS) {
            binding.nextBtn.text = getString(R.string.Reports_Pause)
        } else {
            binding.nextBtn.text = getString(R.string.Reports_Resume)
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
