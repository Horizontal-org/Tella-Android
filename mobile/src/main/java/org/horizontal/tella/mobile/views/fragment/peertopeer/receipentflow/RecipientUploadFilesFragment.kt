package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus
import org.horizontal.tella.mobile.databinding.FragmentUploadFilesBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PeerToPeerParticipant
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.PeerToPeerEndView
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import javax.inject.Inject

@AndroidEntryPoint
class RecipientUploadFilesFragment :
    BaseBindingFragment<FragmentUploadFilesBinding>(FragmentUploadFilesBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    @Inject
    lateinit var p2PSharedState: P2PSharedState

    private lateinit var endView: PeerToPeerEndView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showFormEndView()
        observeUploadProgress()
        viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT

        binding.cancel.setOnClickListener {
            showStandardSheet(
                baseActivity.supportFragmentManager,
                getString(R.string.stop_sharing_files),
                getString(R.string.nearby_sharing_will_be_stopped_the_recipient_will_not_have_access_to_files_that_were_not_fully_transferred),
                getString(R.string.action_continue).uppercase(),
                getString(R.string.stop).uppercase(),
                {},
                {   viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT
                    navManager().navigateFromRecipientUploadFilesFragmentToPeerToPeerResultFragment()})
        }
    }

    private fun showFormEndView() {
        val session = p2PSharedState.session ?: return
        val files = session.files.values.toList()

        endView = PeerToPeerEndView(
            baseActivity,
            session.title
        )

        endView.setFiles(files, MyApplication.isConnectedToInternet(baseActivity), false)

        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
    }

    private fun observeUploadProgress() {

        viewModel.uploadProgress.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe
            val files = state.files
            val percentFloat = state.percent / 100f
            endView.setUploadProgress(files, percentFloat)

            val allSaved = state.files.all { it.status == P2PFileStatus.SAVED }

            if (state.sessionStatus == SessionStatus.FINISHED && allSaved) {
                viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT
                navManager().navigateFromRecipientUploadFilesFragmentToPeerToPeerResultFragment()
            }

        }
    }

}
