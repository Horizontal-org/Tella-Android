package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.databinding.ConnectManuallyVerificationBinding
import org.horizontal.tella.mobile.util.formatHash
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerConnectionInfo
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel
import javax.inject.Inject

class SenderVerificationFragment :
    BaseBindingFragment<ConnectManuallyVerificationBinding>(ConnectManuallyVerificationBinding::inflate) {
    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private lateinit var peerConnectionInfo: PeerConnectionInfo

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initView()
        initObservers()
    }

    private fun initView() {
        binding.confirmAndConnectBtn.setBackgroundResource(R.drawable.bg_round_orange_btn)
        binding.titleDescTextView.text = getString(R.string.make_sure_sequence_matches_recipient)
        binding.warningTextView.text = getString(R.string.sequence_mismatch_warning_recipient)

    }

    private fun initListeners() {
        lifecycleScope.launchWhenStarted {
            viewModel.sessionInfo.collect { session ->
                if (session != null) {
                    peerConnectionInfo = session
                }
                binding.hashContentTextView.text = session?.hash?.formatHash()
            }
        }

        binding.confirmAndConnectBtn.setOnClickListener {
            viewModel.startRegistration(
                ip = peerConnectionInfo.ip,
                port = peerConnectionInfo.port,
                hash = peerConnectionInfo.hash,
                pin = peerConnectionInfo.pin.toString(),
                autoUpload = false
            )
        }

        binding.discardBtn.setOnClickListener {
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }
    }

    private fun initObservers() {


        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                navManager().navigateConnectManuallyVerificationFragmentToprepareUploadFragment()
            } else {
                //  handle error UI
            }
        }
    }
}
