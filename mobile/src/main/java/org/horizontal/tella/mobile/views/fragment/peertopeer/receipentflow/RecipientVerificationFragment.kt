package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import org.horizontal.tella.mobile.databinding.ConnectManuallyVerificationBinding
import org.horizontal.tella.mobile.domain.peertopeer.PeerConnectionPayload
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel

class RecipientVerificationFragment :
    BaseBindingFragment<ConnectManuallyVerificationBinding>(ConnectManuallyVerificationBinding::inflate) {
    private val viewModel: PeerToPeerViewModel by activityViewModels()
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
        lifecycleScope.launchWhenStarted {
            viewModel.sessionInfo.collect { session ->
                binding.hashContentTextView.text = session?.hash
            }
        }
    }

    private fun initListeners() {
        binding.confirmAndConnectBtn.setOnClickListener {

        }
    }

    private fun initObservers() {
        viewModel.registrationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                bundle.putBoolean("isSender", false)
                navManager().navigateFromRecipientVerificationScreenToWaitingFragment()
            } else {
            }
        }
    }
}