package org.horizontal.tella.mobile.views.fragment.googledrive.send

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.views.dialog.googledrive.GoogleDriveConnectFlowActivity
import org.horizontal.tella.mobile.views.fragment.googledrive.GoogleDriveViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsSendFragment
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils

@AndroidEntryPoint
class GoogleDriveSendFragment : BaseReportsSendFragment() {

    override val viewModel by viewModels<GoogleDriveViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState) // Call the base class's onViewCreated

        viewModel.showSharedDriveMigrationSheet.observe(viewLifecycleOwner) { event ->
            if (event != null) {
                viewModel.consumeSharedDriveMigrationEvent()
                BottomSheetUtils.showStandardSheet(
                    baseActivity.supportFragmentManager,
                    getString(R.string.google_drive_shared_drive_migration_sheet_title),
                    getString(R.string.google_drive_shared_drive_migration_sheet_message),
                    getString(R.string.create_new_folder),
                    getString(R.string.action_cancel),
                    {
                        startActivity(Intent(requireContext(), GoogleDriveConnectFlowActivity::class.java))
                    },
                    null
                )
            }
        }

        viewModel.reportProcess.observe(viewLifecycleOwner) { progress ->
            if (progress.second.id == this@GoogleDriveSendFragment.reportInstance?.id) {
                val pct = progress.first
                val instance = progress.second

                pauseResumeLabel(instance)
                endView.setUploadProgress(instance, pct.current.toFloat() / pct.size.toFloat())
            }
        }

        viewModel.instanceProgress.observe(viewLifecycleOwner) { entity ->
            if (entity.id == this@GoogleDriveSendFragment.reportInstance?.id) {
                when (entity.status) {
                    EntityStatus.SUBMITTED -> {
                        viewModel.saveSubmitted(entity)
                        baseActivity.divviupUtils.runGoogleDriveEvent()
                    }

                    EntityStatus.FINALIZED -> {
                        viewModel.saveOutbox(entity)
                    }

                    EntityStatus.PAUSED -> {
                        pauseResumeLabel(entity)
                        viewModel.saveOutbox(entity)
                    }

                    EntityStatus.DELETED -> {
                        viewModel.instanceProgress.postValue(null)
                        handleBackButton()
                    }

                    else -> {
                        this@GoogleDriveSendFragment.reportInstance = entity
                    }
                }
            }
        }

    }

    override fun navigateBack() {
        if (isFromDraft) {
            nav().popBackStack(R.id.newGoogleDriveScreen, true)
        } else {
            nav().popBackStack()
        }
    }

}