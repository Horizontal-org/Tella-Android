package org.horizontal.tella.mobile.views.fragment.peertopeer.receipentflow

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.datatransport.Priority
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.hzontal.tella_locking_ui.ui.pin.pinview.ResourceUtils.getColor
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ConnectHotspotLayoutBinding
import org.horizontal.tella.mobile.util.ConnectionType
import org.horizontal.tella.mobile.util.LocationProvider.isLocationEnabled
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow.PeerToPeerParticipant
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.PeerToPeerViewModel

@AndroidEntryPoint
class ConnectHotspotFragment :
    BaseBindingFragment<ConnectHotspotLayoutBinding>(ConnectHotspotLayoutBinding::inflate) {

    private val viewModel: PeerToPeerViewModel by activityViewModels()
    private var isCheckboxChecked = false

    @RequiresApi(Build.VERSION_CODES.M)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                checkLocationSettings()
            } else {
                baseActivity.showToast(getString(R.string.location_permission_is_required_to_get_wifi_ssid))
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initListeners()
        checkAndRequestPermissions()
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
        if(viewModel.peerToPeerParticipant == PeerToPeerParticipant.RECIPIENT)
        navManager().navigateFromActionConnectHotspotScreenToQrCodeScreen()
        else navManager().navigateFromActionConnectHotspotScreenToScanQrCodeScreen()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(baseActivity, ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            baseActivity.maybeChangeTemporaryTimeout {
                requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
        } else {
            checkLocationSettings()
        }
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    viewModel.updateNetworkInfo()
                }
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(requireActivity(), 1001)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        baseActivity.showToast(getString(R.string.failed_to_open_location_settings))
                    }
                } else {
                    baseActivity.showToast(getString(R.string.location_permission_is_required_to_get_wifi_ssid))
                }
            }
    }

}
