package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.databinding.ConnectManuallyVerificationBinding
import org.horizontal.tella.mobile.util.formatHash
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import org.hzontal.shared_ui.utils.DialogUtils
import javax.inject.Inject

@AndroidEntryPoint
class SenderVerificationFragment :
    BaseBindingFragment<ConnectManuallyVerificationBinding>(ConnectManuallyVerificationBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    @Inject
    lateinit var peerServerStarterManager: PeerServerStarterManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initView()
        initObservers()
    }

    private fun initView() {
        binding.sequenceDescTextView.text = getString(R.string.nearbySharing_verifyConnection_sender)
        binding.confirmAndConnectBtn.setText(getString(R.string.confirm_and_connect))
        binding.hashContentTextView.text = viewModel.p2PState.hash.formatHash()
    }

    private fun initListeners() {
        binding.confirmAndConnectBtn.setOnClickListener {
            // Disable & show waiting immediately
            binding.confirmAndConnectBtn.isEnabled = false
            binding.confirmAndConnectBtn.setText(getString(R.string.waiting_for_the_recipient))
            viewModel.onUserTappedConfirmAndConnect()
        }

        binding.discardBtn.setOnClickListener {
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }
    }

    private fun initObservers() {
        // Manual mode
        viewModel.isManualConnection = true

        // Button enable/disable from VM
        viewModel.canTapConfirm.observe(viewLifecycleOwner) { canTap ->
            binding.confirmAndConnectBtn.isEnabled = canTap
            if (canTap) {
                binding.confirmAndConnectBtn.setText(getString(R.string.confirm_and_connect))
            }
        }
        viewModel.waitingForOtherSide.observe(viewLifecycleOwner) { waiting ->
            if (waiting) {
                binding.confirmAndConnectBtn.isEnabled = false
                binding.confirmAndConnectBtn.setText(getString(R.string.waiting_for_the_recipient))
            }
        }

        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().currentBackStackEntry?.savedStateHandle?.set("registrationSuccess", true)
                navManager().navigateConnectManuallyVerificationFragmentToprepareUploadFragment()
            }
        }

        viewModel.bottomMessageError.observe(viewLifecycleOwner) { message ->
            DialogUtils.showBottomMessage(baseActivity, message, true)
        }

        viewModel.bottomSheetError.observe(viewLifecycleOwner) { (title, description) ->
            showStandardSheet(
                baseActivity.supportFragmentManager,
                title,
                description,
                null,
                getString(R.string.try_again),
                null
            ) {
                // Allow retry: re-enable confirm
                binding.confirmAndConnectBtn.isEnabled = true
                binding.confirmAndConnectBtn.setText(getString(R.string.confirm_and_connect))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        peerServerStarterManager.stopServer()
    }
}
