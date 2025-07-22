package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.databinding.ConnectManuallyVerificationBinding
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.util.formatHash
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import javax.inject.Inject

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

        initListeners()
        initObservers()
    }

    private fun initListeners() = with(binding) {
        toolbar.backClickListener = { navigateBackAndStopServer() }
        discardBtn.setOnClickListener { navigateBackAndStopServer() }

        confirmAndConnectBtn.setOnClickListener(null)
    }

    private fun initObservers() = with(binding) {
        hashContentTextView.text = viewModel.p2PState.hash.formatHash()
        viewModel.p2PState.isUsingManualConnection = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.incomingRequest.collect { request ->
                    if (request != null) {
                        confirmAndConnectBtn.isEnabled = true
                        confirmAndConnectBtn.setText(getString(R.string.confirm_and_connect))
                        confirmAndConnectBtn.setOnClickListener {
                            viewModel.onUserConfirmedRegistration(request.registrationId)
                        }
                    } else {
                        confirmAndConnectBtn.isEnabled = false
                        confirmAndConnectBtn.setOnClickListener(null)
                    }
                }
            }
        }

        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                navManager().navigateFromRecipientVerificationScreenToWaitingReceiverFragment()
            }
        }
    }

    private fun navigateBackAndStopServer() {
        peerServerStarterManager.stopServer()
        navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
    }
}
