package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.databinding.ShowDeviceInfoLayoutBinding
import org.horizontal.tella.mobile.domain.peertopeer.ParsedPeerQr
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionQrCodec
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel
import timber.log.Timber

class ShowDeviceInfoFragment :
    BaseBindingFragment<ShowDeviceInfoLayoutBinding>(ShowDeviceInfoLayoutBinding::inflate) {
    private val viewModel: PeerToPeerViewModel by activityViewModels()

    private var parsedQr: ParsedPeerQr? = null
    private var movedToVerification = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString("payload")?.let { payloadJson ->
            parsedQr = PeerConnectionQrCodec.parse(payloadJson)
        }
        initListeners()
        initView()
        initObservers()
    }

    private fun initView() {
        val parsed = parsedQr
        if (parsed != null) {
            binding.connectCode.setRightText(parsed.ipAddresses.joinToString(", "))
            binding.pin.setRightText(parsed.pin)
            binding.port.setRightText(parsed.port.toString())
        } else {
            binding.connectCode.setRightText(viewModel.p2PState.ip)
            binding.pin.setRightText(viewModel.p2PState.pin)
            binding.port.setRightText(viewModel.p2PState.port)
        }
    }

    private fun initListeners() {
        binding.backBtn.setOnClickListener { back() }
        binding.toolbar.backClickListener = { nav().popBackStack() }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            viewModel.clientHash.collect { clientHash ->
                with(viewModel.p2PState) {
                    val p = parsedQr
                    ip = p?.ipAddresses?.firstOrNull().orEmpty()
                    port = p?.port?.toString().orEmpty()
                    pin = p?.pin
                    hash = clientHash
                }
                moveToVerificationIfNeeded()
            }
        }
    }

    private fun moveToVerificationIfNeeded() {
        if (movedToVerification) return
        movedToVerification = true
        navManager().navigateFromDeviceInfoScreenTRecipientVerificationScreen()
    }
}