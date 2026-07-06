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
            binding.hashContentTextView.setBackgroundResource(R.drawable.bg_verification_hash_step2)
        } else {
            binding.sequenceTitleTextView.text =
                getString(R.string.verification_step1_recipient_hash)
            binding.hashContentTextView.text = viewModel.p2PState.hash.formatHash()
            binding.hashContentTextView.setBackgroundResource(org.hzontal.shared_ui.R.drawable.bg_dual_text_check)
        }
        updateConfirmButton()
    }

    private fun updateConfirmButton() {
        val step = viewModel.p2PState.activeVerificationStep
        val waiting = viewModel.waitingForOtherSide.value == true
        val canTap = viewModel.canTapConfirm.value == true

        if (step == P2PVerificationStep.SENDER_HASH || waiting) {
            applyConfirmButtonState(false, getString(R.string.waiting_for_the_recipient))
        } else {
            applyConfirmButtonState(canTap, getString(R.string.confirm_and_continue))
        }
    }

    private fun applyConfirmButtonState(enabled: Boolean, title: String) {
        binding.confirmAndConnectBtn.isEnabled = enabled
        binding.confirmAndConnectBtn.setBackgroundResource(
            if (enabled) R.drawable.bg_round_orange_btn else R.drawable.bg_round_orange_disabled
        )
        binding.confirmAndConnectBtn.setText(title)
    }

    private fun initListeners() {
        binding.confirmAndConnectBtn.setOnClickListener {
            if (viewModel.p2PState.activeVerificationStep == P2PVerificationStep.SENDER_HASH) return@setOnClickListener
            applyConfirmButtonState(false, getString(R.string.waiting_for_the_recipient))
            viewModel.onUserTappedConfirmAndConnect()
        }

        binding.discardBtn.setOnClickListener {
            viewModel.clearManualConnectionWaitingOnDiscard()
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }
    }

    private fun initObservers() {
        // Manual mode
        viewModel.isManualConnection = true

        // Button enable/disable + label from VM
        viewModel.canTapConfirm.observe(viewLifecycleOwner) {
            updateConfirmButton()
        }
        viewModel.waitingForOtherSide.observe(viewLifecycleOwner) {
            updateConfirmButton()
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
