package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.databinding.ShowDeviceInfoLayoutBinding
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel

class ShowDeviceInfoFragment :
    BaseBindingFragment<ShowDeviceInfoLayoutBinding>(ShowDeviceInfoLayoutBinding::inflate) {
    private val viewModel: PeerToPeerViewModel by activityViewModels()

    private var payload: PeerConnectionPayload? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString("payload")?.let { payloadJson ->
            payload = Gson().fromJson(payloadJson, PeerConnectionPayload::class.java)
        }
        initListeners()
        initView()
        initObservers()
    }

    private fun initView() {
        binding.connectCode.setRightText(payload?.ipAddress)
        binding.pin.setRightText(payload?.pin)
        binding.port.setRightText(payload?.port.toString())
    }

    private fun initListeners() {
        binding.backBtn.setOnClickListener { back() }
        binding.toolbar.backClickListener = { nav().popBackStack() }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            viewModel.clientHash.collect { clientHash ->
                with(viewModel.p2PState) {
                    ip = payload?.ipAddress.toString()
                    port = payload?.port.toString()
                    pin = payload?.pin
                    hash = clientHash
                }
                navManager().navigateFromDeviceInfoScreenTRecipientVerificationScreen()
            }
        }

        viewModel.registrationServerSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Navigate to the next screen
                //    bundle.putBoolean("isSender", false)
                navManager().navigateFromWaitingReceiverFragmentToRecipientSuccessFragment()
                //  reset the LiveData state if we want to consume event once
                viewModel.resetRegistrationState()
            } else {
            }
        }
    }
}