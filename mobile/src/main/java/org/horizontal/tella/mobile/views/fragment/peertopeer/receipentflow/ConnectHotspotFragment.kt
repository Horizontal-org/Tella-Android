package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
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
        viewModel.fetchCurrentNetworkInfo()
    }

    private fun initObservers() {

        viewModel.networkInfo.observe(viewLifecycleOwner) { info ->
            when (info.type) {
                ConnectionType.WIFI -> {
                    binding.currentWifiText.setRightText(info.networkName)
                }

                ConnectionType.CELLULAR -> {
                    binding.currentWifiText.setRightText(info.networkName)
                }

                ConnectionType.NONE -> {
                    binding.currentWifiText.setRightText("No network connected")
                }
            }
        }
    }
}