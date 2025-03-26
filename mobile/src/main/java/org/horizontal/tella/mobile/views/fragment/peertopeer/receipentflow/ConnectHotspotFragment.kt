package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResourceUtils.getColor
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ConnectHotspotLayoutBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerToPeerViewModel
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.ConnectionType

@AndroidEntryPoint
class ConnectHotspotFragment :
    BaseBindingFragment<ConnectHotspotLayoutBinding>(ConnectHotspotLayoutBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by viewModels()

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
                    enableNextButton(ConnectionType.HOTSPOT)
                }

                ConnectionType.WIFI -> {
                    binding.currentWifiText.setRightText(info.networkName)
                    enableNextButton(ConnectionType.WIFI)
                }

                ConnectionType.CELLULAR -> {
                    binding.currentWifiText.setRightText(info.networkName)
                    enableNextButton(ConnectionType.CELLULAR)
                }

                ConnectionType.NONE -> {
                    binding.currentWifiText.setRightText("No network connected")
                    enableNextButton(ConnectionType.NONE)
                }
            }
        }
    }

    private fun initListeners() {

        binding.toolbar.backClickListener = { baseActivity.onBackPressed() }

        binding.nextBtn.setOnClickListener { }

        binding.backBtn.setOnClickListener { baseActivity.onBackPressed() }
    }

    private fun enableNextButton(connectionType: ConnectionType) {
        if (connectionType == ConnectionType.NONE) {
            binding.nextBtn.setOnClickListener { }
            binding.nextBtn.setTextColor(getColor(baseActivity, R.color.wa_white_40))
            binding.currentWifi.setCheckboxEnabled(false)
        } else {
            binding.nextBtn.setOnClickListener { }
            binding.nextBtn.setTextColor(getColor(baseActivity, R.color.wa_white))
            binding.currentWifi.setCheckboxEnabled(true)
        }
    }
}