package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.hzontal.tella_locking_ui.common.extensions.onChange
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.SenderManualConnectionBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils

@AndroidEntryPoint
class SenderManualConnectionFragment :
    BaseBindingFragment<SenderManualConnectionBinding>(SenderManualConnectionBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Do NOT reset the sender identity here: in flow C the receiver has already scanned the
        // sender QR and pinned this session's certificate hash. Regenerating it would make the
        // cert presented at /register differ from the pinned hash → 403 "Sender certificate
        // mismatch". The session is already reset at entry (StartNearBySharing.resetConnectionState).
        initView()
        initListeners()
        initObservers()
    }

    private fun initView() = with(binding) {
        ipAddress.onChange { updateNextButtonState() }
        pin.onChange { updateNextButtonState() }
        port.onChange { updateNextButtonState() }

        updateNextButtonState()
        KeyboardUtil(root)
    }

    private fun initListeners() = with(binding) {
        toolbar.backClickListener = { nav().popBackStack() }

        nextBtn.setOnClickListener {
            val ip = ipAddress.text.toString()
            val port = port.text.toString()
            val pin = this.pin.text.toString()

            viewModel.p2PState.apply {
                this.ip = ip
                this.port = port
                this.pin = pin
            }

            viewModel.handleCertificate(ip, port, pin)
        }
    }

    private fun initObservers() {
        viewModel.getHashSuccess.observe(viewLifecycleOwner) { hash ->
            bundle.putString("payload", hash)
            navManager().navigateFromSenderManualConnectionToConnectManuallyVerification()
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

    private fun isInputValid(): Boolean = with(binding) {
        ipAddress.text?.isNotBlank() == true &&
                pin.text?.isNotBlank() == true
    }

    private fun updateNextButtonState() = with(binding) {
        val enabled = isInputValid()
        nextBtn.isEnabled = enabled
        nextBtn.setTextColor(
            ContextCompat.getColor(
                baseActivity,
                if (enabled) android.R.color.white else android.R.color.darker_gray
            )
        )
    }
}
