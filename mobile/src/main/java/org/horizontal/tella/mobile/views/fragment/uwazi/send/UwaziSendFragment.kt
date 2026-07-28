package org.horizontal.tella.mobile.views.fragment.uwazi.send

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseEntitySendFragment
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.OUTBOX_LIST_PAGE_INDEX
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.SharedLiveData
import org.horizontal.tella.mobile.views.fragment.uwazi.UwaziViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.BUNDLE_IS_FROM_UWAZI_ENTRY
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.UwaziFormEndView

const val SEND_ENTITY = "send_entity"

@AndroidEntryPoint
class UwaziSendFragment : BaseEntitySendFragment<UwaziEntityInstance>() {

    override val viewModel by viewModels<UwaziViewModel>()

    private lateinit var endView: UwaziFormEndView
    private var isFromEntryScreen = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        isFromEntryScreen = arguments?.getBoolean(BUNDLE_IS_FROM_UWAZI_ENTRY) ?: false

        super.onViewCreated(view, savedInstanceState)

        viewModel.progressCallBack.observe(viewLifecycleOwner) { (partName, total) ->
            endView.showUploadProgress(partName)
            endView.setUploadProgress(partName, total)
        }

        viewModel.instanceProgress.observe(viewLifecycleOwner) { entity ->
            when (entity.status) {
                EntityStatus.SUBMISSION_IN_PROGRESS,
                EntityStatus.SUBMISSION_PENDING -> pauseResumeLabel(entity)

                EntityStatus.SUBMITTED -> baseActivity.divviupUtils.runUwaziSentEvent()

                EntityStatus.SUBMISSION_ERROR -> {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        getString(R.string.collect_toast_fail_sending_form),
                        true
                    )
                    // Leave without the shared back handling, which would overwrite the error
                    // status the ViewModel just persisted with SUBMISSION_PENDING.
                    SharedLiveData.updateViewPagerPosition.postValue(OUTBOX_LIST_PAGE_INDEX)
                    navigateBack()
                }

                else -> {}
            }
        }
    }

    override fun readInstanceFromArguments(arguments: Bundle): UwaziEntityInstance? =
        Gson().fromJson(arguments.getString(SEND_ENTITY), UwaziEntityInstance::class.java)

    override fun showFormEndView() {
        val instance = reportInstance ?: return

        endView = UwaziFormEndView(baseActivity, getFormattedFormTitle(instance))
        endView.setInstance(instance, false, false)
        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
    }

    /**
     * Uwazi reports its sent event once the upload is confirmed, in the [UwaziViewModel.instanceProgress]
     * observer above, rather than when the send screen opens.
     */
    override fun trackSentEvent() = Unit

    override fun getPendingMessage(): String = getString(R.string.Report_Available_in_Outbox)

    override fun getSubmittedMessage(): String =
        getString(R.string.form_successfully_submitted, reportInstance?.title.orEmpty())

    override fun navigateBack() {
        if (isFromEntryScreen) {
            nav().popBackStack(R.id.uwaziEntryScreen, true)
        } else {
            nav().popBackStack()
        }
    }

    private fun getFormattedFormTitle(entityInstance: UwaziEntityInstance): String {
        return getString(R.string.Uwazi_Server_Title) + " " +
                entityInstance.collectTemplate?.serverName + "\n" +
                getString(R.string.Uwazi_Template_Title) + " " +
                entityInstance.collectTemplate?.entityRow?.translatedName
    }
}
