package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResourceUtils.getColor
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ConnectHotspotLayoutBinding
import org.horizontal.tella.mobile.util.ConnectionType
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel

@AndroidEntryPoint
class ConnectHotspotFragment :
    BaseBindingFragment<ConnectHotspotLayoutBinding>(ConnectHotspotLayoutBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var isCheckboxChecked = false
    private val permissionsToRequest = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.M)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[ACCESS_FINE_LOCATION] ?: false
            val wifiStateGranted = permissions[ACCESS_WIFI_STATE] ?: false
            val networkStateGranted = permissions[ACCESS_NETWORK_STATE] ?: false

            if (fineLocationGranted && wifiStateGranted && networkStateGranted) {
                viewModel.updateNetworkInfo()
            } else {
                baseActivity.showToast("Location and network permissions are required to get WiFi SSID.")
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initListeners()
        checkAndRequestPermissions()
        updateInfoOrGetPermission()
    }

    private fun initObservers() {
        viewModel.networkInfo.observe(viewLifecycleOwner) { info ->
            viewModel.currentNetworkInfo = info
            when (info.connectionType) {
                ConnectionType.HOTSPOT, ConnectionType.WIFI, ConnectionType.CELLULAR -> {
                    binding.currentWifiText.setRightText(info.ssid ?: "Unknown")
                    updateNextButtonState(info.connectionType)
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
            val currentType = viewModel.networkInfo.value?.connectionType ?: ConnectionType.NONE
            updateNextButtonState(currentType)
        }

        binding.toolbar.backClickListener = { baseActivity.onBackPressed() }
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndRequestPermissions() {

        if (ContextCompat.checkSelfPermission(
                baseActivity,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                baseActivity,
                ACCESS_WIFI_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(ACCESS_WIFI_STATE)
        }
        if (ContextCompat.checkSelfPermission(
                baseActivity,
                ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(ACCESS_NETWORK_STATE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateInfoOrGetPermission() {
        if (permissionsToRequest.isNotEmpty()) {
            baseActivity.maybeChangeTemporaryTimeout {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } else {
            viewModel.updateNetworkInfo()
        }
    }
}
