package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PVerificationStep
import org.horizontal.tella.mobile.databinding.ConnectManuallyVerificationBinding
import org.horizontal.tella.mobile.util.formatHash
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import javax.inject.Inject

@AndroidEntryPoint
class RecipientVerificationFragment :
    BaseBindingFragment<ConnectManuallyVerificationBinding>(ConnectManuallyVerificationBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initListeners()
        initObservers()
    }

    private fun initUI() = with(binding) {
        refreshVerificationUi()
        confirmAndConnectBtn.isEnabled = true
    }

    private fun refreshVerificationUi() = with(binding) {
        val step = viewModel.p2PState.activeVerificationStep
        if (step == P2PVerificationStep.SENDER_HASH) {
            sequenceDescTextView.text = getString(R.string.verification_step2_sender_hash)
            hashContentTextView.text = viewModel.p2PState.pinnedSenderHash
                .ifBlank { viewModel.p2PState.localSenderHash }
                .formatHash()
        } else {
            sequenceDescTextView.text = getString(R.string.verification_step1_recipient_hash)
            hashContentTextView.text = viewModel.p2PState.localReceiverHash
                .ifBlank { viewModel.p2PState.hash }
                .formatHash()
        }
        confirmAndConnectBtn.setText(
            if (step == P2PVerificationStep.SENDER_HASH) {
                getString(R.string.confirm_and_connect)
            } else {
                getString(R.string.confirm_and_continue)
            }
        )
    }

    private fun initListeners() = with(binding) {
        toolbar.backClickListener = { navigateBackAndStopServer() }
        discardBtn.setOnClickListener { navigateBackAndStopServer() }

        // Tap immediately — even if no incoming request yet
        confirmAndConnectBtn.setOnClickListener {
            confirmAndConnectBtn.isEnabled = false
            confirmAndConnectBtn.setText(getString(R.string.waiting_for_the_sender))
            viewModel.onRecipientConfirmTapped()
        }
    }

    private fun initObservers() = with(binding) {
        // Manual mode so we don't auto-accept
        viewModel.p2PState.isUsingManualConnection = true


        // Navigate ONLY after server confirms (both sides done)
        viewModel.registrationServerSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                navManager().navigateFromRecipientVerificationScreenToWaitingReceiverFragment()
            }
        }

        viewModel.getHashSuccess.observe(viewLifecycleOwner) {
            refreshVerificationUi()
        }

        viewModel.incompatibleProtocolError.observe(viewLifecycleOwner) {
            navigateBackAndStopServer()
        }

        viewModel.closeConnection.observe(viewLifecycleOwner) { closeConnection ->
            if (closeConnection) navigateBackAndStopServer()
        }

        // Optional: reflect VM UI flags if you want the button text/state to be VM-driven
        viewModel.waitingForOtherSide.observe(viewLifecycleOwner) { waiting ->
            if (waiting) {
                confirmAndConnectBtn.isEnabled = false
                confirmAndConnectBtn.setText(getString(R.string.waiting_for_the_sender))
            }
        }
        viewModel.canTapConfirm.observe(viewLifecycleOwner) { canTap ->
            if (canTap) {
                confirmAndConnectBtn.isEnabled = true
                confirmAndConnectBtn.setText(getString(R.string.confirm_and_connect))
            }
        }
    }

    private fun navigateBackAndStopServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                peerServerStarterManager.stopServer()
            }
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }
    }


}
