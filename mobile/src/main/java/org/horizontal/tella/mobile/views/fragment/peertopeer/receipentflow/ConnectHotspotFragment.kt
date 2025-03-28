package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResourceUtils.getColor
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.certificate.CertificateGenerator
import org.horizontal.tella.mobile.databinding.ConnectHotspotLayoutBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.ConnectionType
import timber.log.Timber

@AndroidEntryPoint
class ConnectHotspotFragment :
    BaseBindingFragment<ConnectHotspotLayoutBinding>(ConnectHotspotLayoutBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var isCheckboxChecked = false

    companion object {
        @JvmStatic
        fun newInstance(): ConnectHotspotFragment {
            val frag = ConnectHotspotFragment()
            val args = Bundle()
            frag.arguments = args
            return frag
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initListeners()
        viewModel.fetchCurrentNetworkInfo()
    }

    private fun initObservers() {

        viewModel.networkInfo.observe(viewLifecycleOwner) { info ->
            when (info.type) {
                ConnectionType.HOTSPOT -> {
                    binding.currentWifiText.setRightText(info.networkName)
                    updateNextButtonState(ConnectionType.HOTSPOT)
                }

                ConnectionType.WIFI -> {
                    binding.currentWifiText.setRightText(info.networkName)
                    updateNextButtonState(ConnectionType.WIFI)
                }

                ConnectionType.CELLULAR -> {
                    binding.currentWifiText.setRightText(info.networkName)
                    updateNextButtonState(ConnectionType.CELLULAR)
                }

                ConnectionType.NONE -> {
                    binding.currentWifiText.setRightText("No network connected")
                    updateNextButtonState(ConnectionType.NONE)
                }
            }
        }
    }

    private fun initListeners() {

        binding.currentWifi.setOnCheckedChangeListener { isChecked ->
            isCheckboxChecked = isChecked
            updateNextButtonState(viewModel.currentNetworkInfo?.type)
        }

        binding.toolbar.backClickListener = { baseActivity.onBackPressed() }

        binding.nextBtn.setOnClickListener { }

        binding.backBtn.setOnClickListener { baseActivity.onBackPressed() }
    }

    private fun updateNextButtonState(connectionType: ConnectionType?) {
        val isEligibleConnection =
            connectionType != ConnectionType.NONE && connectionType != ConnectionType.CELLULAR
        val shouldEnable = isEligibleConnection && isCheckboxChecked

        binding.nextBtn.setOnClickListener(if (shouldEnable) {
            { onNextClicked() }
        } else {
            { }
        })
        binding.nextBtn.setTextColor(
            getColor(baseActivity, if (shouldEnable) R.color.wa_white else R.color.wa_white_40)
        )
        binding.currentWifi.setCheckboxEnabled(isEligibleConnection)
    }

    private fun onNextClicked() {
        navManager().navigateFromActionConnectHotspotScreenToQrCodeScreen()
    }

}