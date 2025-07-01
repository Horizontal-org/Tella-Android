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
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel
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

    private fun initListeners() {

        binding.toolbar.backClickListener = {
            peerServerStarterManager.stopServer()
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }

        binding.discardBtn.setOnClickListener {
            peerServerStarterManager.stopServer()
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }

        binding.confirmAndConnectBtn.setOnClickListener(null)
    }

    private fun initObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.sessionInfo.collect { session ->
                binding.hashContentTextView.text = session?.hash?.formatHash()
            }
        }
        viewModel.isManualConnection = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.incomingRequest.collect { request ->
                    if (request != null) {
                        binding.confirmAndConnectBtn.isEnabled = true
                        binding.confirmAndConnectBtn.setBackgroundResource(R.drawable.bg_round_orange_btn)

                        // Set the click listener when the request is available
                        binding.confirmAndConnectBtn.setOnClickListener {
                            viewModel.onUserConfirmedRegistration(request.registrationId)
                        }

                        binding.discardBtn.setOnClickListener {
                            peerServerStarterManager.stopServer()
                            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
                            viewModel.onUserRejectedRegistration(request.registrationId)
                        }
                    } else {
                        binding.confirmAndConnectBtn.isEnabled = false
                        binding.confirmAndConnectBtn.setBackgroundResource(R.drawable.bg_round_orange16_btn)
                        binding.confirmAndConnectBtn.setOnClickListener(null) // Optional: clear listener
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

}