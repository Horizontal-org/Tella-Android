package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.databinding.ConnectManuallyVerificationBinding
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.util.formatHash
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import javax.inject.Inject

@AndroidEntryPoint
class RecipientVerificationFragment :
    BaseBindingFragment<ConnectManuallyVerificationBinding>(ConnectManuallyVerificationBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var payload: PeerConnectionPayload? = null

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("payload")?.let { payloadJson ->
            payload = Gson().fromJson(payloadJson, PeerConnectionPayload::class.java)
        }

        initUI()
        initListeners()
        initObservers()
    }

    private fun initUI() = with(binding) {
        // Simpler instruction text (update your string as needed)
        sequenceDescTextView.text = getString(R.string.nearbySharing_verifyConnection_recipient)
        hashContentTextView.text = viewModel.p2PState.hash.formatHash()

        // IMPORTANT: button is enabled immediately
        confirmAndConnectBtn.isEnabled = true
        confirmAndConnectBtn.setText(getString(R.string.confirm_and_connect))
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
        peerServerStarterManager.stopServer()
        navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
    }


}
