package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
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
    private lateinit var endView: PeerToPeerEndView
    private val progressPercentLiveData = MutableLiveData<Int>()
    private var sheetShown = false

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUI()
        setupToolbar()
        setupCancelButton()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showStopSharingConfirmation()
        }
    }

    private fun initializeUI() {
        showFormEndView()
        observeUploadProgress()
        // observeBottomSheetProgress()
        viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT
    }

    private fun setupToolbar() {
        binding.toolbar.setStartTextTitle(getString(R.string.receiving_and_encrypting_files))
        binding.toolbar.backClickListener = { showStopSharingConfirmation() }
    }

    private fun setupCancelButton() {
        binding.cancel.setOnClickListener { showStopSharingConfirmation() }
    }

    private fun showStopSharingConfirmation() {
        showStandardSheet(
            baseActivity.supportFragmentManager,
            getString(R.string.nearbysharing_stop_receiving_files),
            getString(R.string.nearbysharing_stop_receiving_files_description),
            getString(R.string.action_continue).uppercase(),
            getString(R.string.stop).uppercase(),
            onConfirmClick = {},
            onCancelClick = { stopServerAndNavigate() }
        )
    }

    private fun stopServerAndNavigate() {
        peerServerStarterManager.stopServer()
        viewModel.peerToPeerParticipant = PeerToPeerParticipant.RECIPIENT
        navManager().navigateFromRecipientUploadFilesFragmentToPeerToPeerResultFragment()
    }

    private fun showFormEndView() {
        val session = viewModel.p2PState.session ?: return
        val files = session.files.values.toList()

        endView = PeerToPeerEndView(baseActivity, session.title)
        endView.setFiles(files, MyApplication.isConnectedToInternet(baseActivity), false)

        binding.endViewContainer.removeAllViews()
        binding.endViewContainer.addView(endView)
    }

    private fun observeUploadProgress() {
        viewModel.uploadProgress.observe(viewLifecycleOwner) { state ->
            if (state == null) return@observe

            val files = state.files
            val percent = state.percent.coerceIn(0, 100)
            endView.setUploadProgress(files, percent / 100f)

            val isTerminal = when (state.sessionStatus) {
                SessionStatus.FINISHED,
                SessionStatus.FINISHED_WITH_ERRORS,
                SessionStatus.CLOSED -> true

                else -> false  // WAITING, SENDING, SAVING → not yet
            }

            if (isTerminal) {
                // Sheet only auto-dismisses when progress == file count; hash/save failures can leave savedCount at 0.
                if (files.isNotEmpty()) {
                    progressPercentLiveData.value = files.size
                }
                stopServerAndNavigate()
            }
        }
    }


}
