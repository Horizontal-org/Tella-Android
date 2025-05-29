package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import org.horizontal.tella.mobile.databinding.ConnectManuallyVerificationBinding
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class ConnectManuallyVerificationFragment:
    BaseBindingFragment<ConnectManuallyVerificationBinding>(ConnectManuallyVerificationBinding::inflate) {

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

    }

    private fun initListeners() {

    }
}