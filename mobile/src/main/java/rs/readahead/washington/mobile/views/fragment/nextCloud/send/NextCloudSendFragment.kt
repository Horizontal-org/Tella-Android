package rs.readahead.washington.mobile.views.fragment.nextCloud.send

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.views.fragment.nextCloud.NextCloudViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsSendFragment

@AndroidEntryPoint
class NextCloudSenFragment : BaseReportsSendFragment() {

    override val viewModel by viewModels<NextCloudViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState) // Call the base class's onViewCreated

        viewModel.reportProcess.observe(viewLifecycleOwner) { progress ->
            binding.progress.isVisible = false

            if (progress.second.id == this@NextCloudSenFragment.reportInstance?.id) {
                val pct = progress.first
                val instance = progress.second

                pauseResumeLabel(instance)
                endView.setUploadProgress(instance, pct.current.toFloat() / pct.size.toFloat())
            }
        }
        binding.progress.isVisible = true

        viewModel.instanceProgress.observe(viewLifecycleOwner) { entity ->

            if (entity.id == this@NextCloudSenFragment.reportInstance?.id) {
                when (entity.status) {
                    EntityStatus.SUBMITTED -> {
                        viewModel.saveSubmitted(entity)
                        baseActivity.divviupUtils.runNextCloudEvent()
                        binding.progress.isVisible = false

                    }

                    EntityStatus.FINALIZED -> {
                        viewModel.saveOutbox(entity)
                        binding.progress.isVisible = false

                    }

                    EntityStatus.PAUSED -> {
                        pauseResumeLabel(entity)
                        viewModel.saveOutbox(entity)
                        binding.progress.isVisible = false

                    }

                    EntityStatus.DELETED -> {
                        viewModel.instanceProgress.postValue(null)
                        handleBackButton()
                        binding.progress.isVisible = false
                    }

                    else -> {
                        this@NextCloudSenFragment.reportInstance = entity
                    }
                }
            }
        }


    }

    override fun navigateBack() {
        if (isFromDraft) {
            nav().popBackStack(R.id.newNextCloudScreen, true)
        } else {
            nav().popBackStack()
        }
    }


}