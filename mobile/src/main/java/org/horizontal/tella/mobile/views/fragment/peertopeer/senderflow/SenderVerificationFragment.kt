package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerServerStarterManager
import org.horizontal.tella.mobile.databinding.ConnectManuallyVerificationBinding
import org.horizontal.tella.mobile.util.formatHash
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import org.hzontal.shared_ui.utils.DialogUtils
import javax.inject.Inject

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
        binding.warningTextView.text = getString(R.string.hash_sender_description)
        binding.confirmAndConnectBtn.setText(getString(R.string.confirm_and_connect))
    }

    private fun initListeners() {

        binding.hashContentTextView.text = viewModel.p2PState.hash.formatHash()

        binding.confirmAndConnectBtn.setOnClickListener {
            binding.confirmAndConnectBtn.setText(getString(R.string.waiting_for_the_recipient))
            viewModel.startRegistration(
                ip = viewModel.p2PState.ip,
                port = viewModel.p2PState.port,
                hash = viewModel.p2PState.hash,
                pin = viewModel.p2PState.pin.toString()
            )
        }

        binding.discardBtn.setOnClickListener {
            navManager().navigateBackToStartNearBySharingFragmentAndClearBackStack()
        }
    }

    private fun initObservers() {
        viewModel.isManualConnection = true
        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.set("registrationSuccess", true)
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
                null,
                {
                    viewModel.startRegistration(
                        ip = viewModel.p2PState.ip,
                        port = viewModel.p2PState.port,
                        hash = viewModel.p2PState.hash,
                        pin = viewModel.p2PState.pin.toString()
                    )
                })
        }
    }
}
