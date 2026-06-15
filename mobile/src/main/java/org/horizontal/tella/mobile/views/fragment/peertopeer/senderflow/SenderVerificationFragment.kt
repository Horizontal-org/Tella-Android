package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PVerificationStep
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
        refreshVerificationUi()
    }

    private fun refreshVerificationUi() {
        val step = viewModel.p2PState.activeVerificationStep
        binding.sequenceDescTextView.text =
            getString(R.string.nearbySharing_verifyConnection_sender)
        if (step == P2PVerificationStep.SENDER_HASH) {
            binding.sequenceTitleTextView.text = getString(R.string.verification_step2_sender_hash)
            binding.hashContentTextView.text = viewModel.p2PState.localSenderHash.formatHash()
        } else {
            binding.sequenceTitleTextView.text =
                getString(R.string.verification_step1_recipient_hash)
            binding.hashContentTextView.text = viewModel.p2PState.hash.formatHash()
        }
        setConfirmButtonForStep(step)
    }

    private fun setConfirmButtonForStep(step: P2PVerificationStep?) {
        binding.confirmAndConnectBtn.setText(
            if (step == P2PVerificationStep.SENDER_HASH) {
                getString(R.string.confirm_and_connect)
            } else {
                getString(R.string.confirm_and_continue)
            }
        )
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
                setConfirmButtonForStep(viewModel.p2PState.activeVerificationStep)
            }
        }
        viewModel.waitingForOtherSide.observe(viewLifecycleOwner) { waiting ->
            if (waiting) {
                binding.confirmAndConnectBtn.isEnabled = false
                binding.confirmAndConnectBtn.setText(getString(R.string.waiting_for_the_recipient))
            }
        }

        viewModel.getHashSuccess.observe(viewLifecycleOwner) {
            refreshVerificationUi()
        }

        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().currentBackStackEntry?.savedStateHandle?.set("registrationSuccess", true)
                navManager().navigateConnectManuallyVerificationFragmentToprepareUploadFragment()
            }
        }

        viewModel.bottomMessageError.observe(viewLifecycleOwner) { message ->
            DialogUtils.showBottomMessage(baseActivity, message, false)
        }

        viewModel.bottomSheetError.observe(viewLifecycleOwner) { (title, description) ->
            showStandardSheet(
                baseActivity.supportFragmentManager,
                title,
                description,
                getString(R.string.action_ok),
                null,
                onConfirmClick = {
                    viewModel.resetRegistrationState()
                    navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
                },
                onCancelClick = null,
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        peerServerStarterManager.stopServer()
    }
}
