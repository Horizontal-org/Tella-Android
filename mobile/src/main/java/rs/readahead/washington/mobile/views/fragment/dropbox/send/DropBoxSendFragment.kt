package rs.readahead.washington.mobile.views.fragment.dropbox.send

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.views.dialog.dropbox.DropBoxConnectFlowActivity
import rs.readahead.washington.mobile.views.dialog.dropbox.utils.DropboxOAuthUtil
import rs.readahead.washington.mobile.views.fragment.dropbox.DropBoxViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsSendFragment
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.SharedLiveData.refreshTokenServer
import javax.inject.Inject

const val REFRESH_SERVER_INTENT = "refresh_server_intent"

@AndroidEntryPoint
class DropBoxSendFragment : BaseReportsSendFragment() {
    @Inject
    lateinit var gson: Gson

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
                        baseActivity.divviupUtils.runDropBoxEvent()
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

                    EntityStatus.SUBMISSION_PENDING -> {
                        viewModel.saveOutbox(entity)
                    }

                    else -> {
                        this@DropBoxSendFragment.reportInstance = entity
                    }
                }
            }
        }
        viewModel.tokenExpired.observe(viewLifecycleOwner) { refreshDropBoxServer ->
            if (refreshDropBoxServer != null) {
                val intent = Intent(baseActivity, DropBoxConnectFlowActivity::class.java)
                intent.putExtra(REFRESH_SERVER_INTENT, gson.toJson(refreshDropBoxServer))
                startActivity(intent)
            }
        }

        refreshTokenServer.observe(viewLifecycleOwner){
            reportInstance?.let { viewModel.submitReport(it,false) }
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