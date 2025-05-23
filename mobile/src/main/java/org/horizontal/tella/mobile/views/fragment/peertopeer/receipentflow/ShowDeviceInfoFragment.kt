package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import org.horizontal.tella.mobile.databinding.ShowDeviceInfoLayoutBinding
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class ShowDeviceInfoFragment :
    BaseBindingFragment<ShowDeviceInfoLayoutBinding>(ShowDeviceInfoLayoutBinding::inflate) {

    private var payload: PeerConnectionPayload? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString("payload")?.let { payloadJson ->
            payload = Gson().fromJson(payloadJson, PeerConnectionPayload::class.java)
        }
        initListeners()
        initView()
    }

    private fun initView() {
        binding.connectCode.setRightText(payload?.connectCode)
        binding.pin.setRightText(payload?.pin)
        binding.port.setRightText(payload?.port.toString())
    }

    private fun initListeners() {

    }
}