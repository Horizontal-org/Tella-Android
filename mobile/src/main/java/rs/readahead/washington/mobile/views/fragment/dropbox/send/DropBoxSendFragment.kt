package rs.readahead.washington.mobile.views.fragment.dropbox.send

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.views.dialog.dropbox.DropBoxConnectFlowActivity
import rs.readahead.washington.mobile.views.dialog.dropbox.utils.DropboxOAuthUtil
import rs.readahead.washington.mobile.views.fragment.dropbox.DropBoxViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsSendFragment
import javax.inject.Inject

@AndroidEntryPoint
class DropBoxSendFragment : BaseReportsSendFragment() {

    @Inject
    lateinit var dropBoxUtil: DropboxOAuthUtil

    override val viewModel by viewModels<DropBoxViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState) // Call the base class's onViewCreated

        viewModel.reportProcess.observe(viewLifecycleOwner) { progress ->
            if (progress.second.id == this@DropBoxSendFragment.reportInstance?.id) {
                val pct = progress.first
                val instance = progress.second

                pauseResumeLabel(instance)
                endView.setUploadProgress(instance, pct.current.toFloat() / pct.size.toFloat())
            }
        }

        viewModel.instanceProgress.observe(viewLifecycleOwner) { entity ->
            if (entity.id == this@DropBoxSendFragment.reportInstance?.id) {
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
                        this@DropBoxSendFragment.reportInstance = entity
                    }
                }
            }
        }
        viewModel.tokenExpired.observe(viewLifecycleOwner) { dropBoxServer ->
            if (dropBoxServer != null) {
                dropBoxUtil.startDropboxAuthorizationOAuth2(baseActivity)
            }
        }

    }

    override fun navigateBack() {
        if (isFromDraft) {
            nav().popBackStack(R.id.newdropBoxScreen, true)
        } else {
            nav().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
    }


}