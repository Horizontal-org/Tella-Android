package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.hzontal.tella_locking_ui.common.extensions.onChange
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.SenderManualConnectionBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showStandardSheet
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils

// TODO: Show errors in the bottom sheet
class SenderManualConnectionFragment :
    BaseBindingFragment<SenderManualConnectionBinding>(SenderManualConnectionBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        backBtn.setOnClickListener { nav().popBackStack() }
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
                 viewModel.handleCertificate(viewModel.p2PState.ip, viewModel.p2PState.port, viewModel.p2PState.pin.toString())
             })
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
