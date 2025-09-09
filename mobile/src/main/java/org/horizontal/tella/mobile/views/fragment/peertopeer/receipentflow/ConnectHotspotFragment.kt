package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResourceUtils.getColor
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ConnectHotspotLayoutBinding
import org.horizontal.tella.mobile.util.ConnectionType
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PeerToPeerParticipant
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel

@AndroidEntryPoint
class ConnectHotspotFragment :
    BaseBindingFragment<ConnectHotspotLayoutBinding>(ConnectHotspotLayoutBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var isCheckboxChecked = false


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.updateNetworkInfo()
        initObservers()
        initListeners()
    }

    private fun initObservers() {
        viewModel.networkInfo.observe(viewLifecycleOwner) { info ->
            viewModel.currentNetworkInfo = info
            when (info.connectionType) {
                ConnectionType.HOTSPOT, ConnectionType.WIFI, ConnectionType.CELLULAR -> {
                    updateNextButtonState(info.connectionType)
                }

                ConnectionType.NONE -> {
                    updateNextButtonState(ConnectionType.NONE)
                }
            }
        }
    }

    private fun initListeners() {
        binding.currentWifi.setOnCheckedChangeListener { isChecked ->
            isCheckboxChecked = isChecked
            val currentType = viewModel.networkInfo.value?.connectionType ?: ConnectionType.NONE
            updateNextButtonState(currentType)
        }

        binding.toolbar.backClickListener = { baseActivity.onBackPressed() }
        binding.tipsCard.setOnTipsClick {
            navManager().navigateFromConnectHotspotScreenToTipsToConnectFragment()
        }
    }

    private fun updateNextButtonState(connectionType: ConnectionType?) {
        val isEligibleConnection =
            connectionType == ConnectionType.WIFI || connectionType == ConnectionType.HOTSPOT

        binding.currentWifi.setCheckboxEnabled(isEligibleConnection)

        val shouldEnable = isEligibleConnection && isCheckboxChecked

        binding.nextBtn.isEnabled = shouldEnable
        binding.nextBtn.isClickable = shouldEnable

        binding.nextBtn.alpha = if (shouldEnable) 1f else 0.5f

        binding.nextBtn.setOnClickListener(
            if (shouldEnable) { { onNextClicked() } } else { { /* no-op */ } }
        )

        binding.nextBtn.setTextColor(
            getColor(baseActivity, if (shouldEnable) R.color.wa_white else R.color.wa_white_40)
        )
    }


    private fun onNextClicked() {
        if (viewModel.peerToPeerParticipant == PeerToPeerParticipant.RECIPIENT)
            navManager().navigateFromActionConnectHotspotScreenToQrCodeScreen()
        else navManager().navigateFromActionConnectHotspotScreenToScanQrCodeScreen()

    }

}
