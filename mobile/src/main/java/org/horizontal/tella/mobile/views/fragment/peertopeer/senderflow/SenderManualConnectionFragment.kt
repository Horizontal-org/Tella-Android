package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.hzontal.tella_locking_ui.common.extensions.onChange
import org.horizontal.tella.mobile.databinding.SenderManualConnectionBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerConnectionInfo
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil

class SenderManualConnectionFragment :
    BaseBindingFragment<SenderManualConnectionBinding>(SenderManualConnectionBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()
        initView()
    }

    private fun initView() {
        binding.connectCode.onChange {
            updateNextButtonState()
        }
        binding.port.onChange {
            updateNextButtonState()
        }

        binding.port.onChange {
            updateNextButtonState()
        }

        initObservers()

        updateNextButtonState()

        KeyboardUtil(binding.root)

    }

    private fun initListeners() {
        binding.backBtn.setOnClickListener {
            nav().popBackStack()
        }

        binding.nextBtn.setOnClickListener {

        }
    }

    private fun isInputValid(): Boolean {
        return binding.connectCode.text?.isNotBlank() == true &&
                binding.pin.text?.isNotBlank() == true &&
                binding.port.text?.isNotBlank() == true
    }

    private fun updateNextButtonState() {
        val enabled = isInputValid()
        binding.nextBtn.isEnabled = enabled
        binding.nextBtn.setTextColor(
            ContextCompat.getColor(
                baseActivity,
                if (enabled) android.R.color.white else android.R.color.darker_gray
            )
        )
        binding.toolbar.backClickListener = { nav().popBackStack() }
        binding.backBtn.setOnClickListener { nav().popBackStack() }
        binding.nextBtn.setOnClickListener {
            val ip = binding.connectCode.text.toString()
            val port = binding.port.text.toString()
            val pin = binding.pin.text.toString()

            viewModel.setPeerSessionInfo(
                PeerConnectionInfo(
                    ip = ip,
                    port = port,
                    pin = pin.toInt()
                )
            ) // Store values

            viewModel.handleCertificate(
                ip = binding.connectCode.text.toString(),
                port = binding.port.text.toString(),
                pin = binding.pin.text.toString()
            )
        }
    }

    private fun initObservers() {
        viewModel.getHashSuccess.observe(viewLifecycleOwner) { hash ->
            bundle.putString("payload", hash)
            viewModel.setPeerSessionInfo(
                PeerConnectionInfo(
                    ip = binding.connectCode.text.toString(),
                    port = binding.port.text.toString(),
                    pin = binding.pin.text.toString().toInt(),
                    hash = hash
                )
            )

            navManager().navigateFromSenderManualConnectionToConnectManuallyVerification()
        }

        viewModel.getHashError.observe(viewLifecycleOwner) { error ->
            error.localizedMessage?.let { showToast(it) }
        }
    }

}