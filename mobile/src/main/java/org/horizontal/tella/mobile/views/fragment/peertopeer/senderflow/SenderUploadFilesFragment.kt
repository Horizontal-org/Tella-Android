package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus
import org.horizontal.tella.mobile.databinding.FragmentUploadFilesBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.FileTransferViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.PeerToPeerEndView
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet

class SenderUploadFilesFragment :
    BaseBindingFragment<FragmentUploadFilesBinding>(FragmentUploadFilesBinding::inflate) {

    private val viewModel: FileTransferViewModel by activityViewModels()
    private lateinit var endView: PeerToPeerEndView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.uploadAllFiles()
        viewModel.peerToPeerParticipant = PeerToPeerParticipant.SENDER
        showFormEndView()
        observeUploadProgress()
        viewModel.uploadAllFiles()
        binding.cancel.setOnClickListener {
            showStandardSheet(
                baseActivity.supportFragmentManager,
                getString(R.string.stop_sharing_files),
                getString(R.string.nearby_sharing_will_be_stopped_the_recipient_will_not_have_access_to_files_that_were_not_fully_transferred),
                getString(R.string.action_continue).uppercase(),
                getString(R.string.stop).uppercase(),
                {},
                {})
        }
    }

    private fun showFormEndView() {
        val session = viewModel.p2PSharedState.session ?: return
        val files = session.files.values.toList()

        endView = PeerToPeerEndView(
            baseActivity, session.title ?: "Transfer"
        )

        endView.setFiles(files, MyApplication.isConnectedToInternet(baseActivity), false)
        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
        endView.clearPartsProgress(files, session.status)
    }

    private fun observeUploadProgress() {
        viewModel.uploadProgress.observe(viewLifecycleOwner) { state ->
            val files = state.files
            val percentFloat = state.percent / 100f
            endView.setUploadProgress(files, percentFloat)


            val allFinished = state.files.all { it.status == P2PFileStatus.FINISHED }

            if (state.sessionStatus == SessionStatus.FINISHED && allFinished) {
                navManager().navigateFromUploadSenderFragmentToPeerToPeerResultFragment()
            }
        }
    }
}
