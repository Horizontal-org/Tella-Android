package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.ConnectionType
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.NetworkInfo
import javax.inject.Inject

@HiltViewModel
class PeerToPeerViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo

    @SuppressLint("MissingPermission", "DiscouragedPrivateApi")
    fun fetchCurrentNetworkInfo() {
        val connectivityManager =
            ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)

        when {
            capabilities == null -> {
                _networkInfo.value = NetworkInfo(ConnectionType.NONE, null, null)
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo

                val ssid =
                    if (!wifiInfo.ssid.isNullOrEmpty() && wifiInfo.ssid != WifiManager.UNKNOWN_SSID) {
                        wifiInfo.ssid.trim('"')
                    } else {
                        getWifiSsidFromCapabilities(capabilities) ?: "Hotspot"
                    }

                val ipAddress = Formatter.formatIpAddress(wifiInfo.ipAddress)
                _networkInfo.value = NetworkInfo(ConnectionType.WIFI, ssid, ipAddress)
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val hotspotSSID = getDeviceHotspotSSID(context) ?: "Hotspot"
                _networkInfo.value = NetworkInfo(ConnectionType.CELLULAR, hotspotSSID, null)
            }

            else -> {
                _networkInfo.value = NetworkInfo(ConnectionType.NONE, null, null)
            }
        }
    }


    private fun getWifiSsidFromCapabilities(capabilities: NetworkCapabilities): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (capabilities.transportInfo as? WifiInfo)?.ssid?.trim('"')
        } else {
            null
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getDeviceHotspotSSID(context: Context): String? {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("getWifiApConfiguration")
            method.isAccessible = true
            val wifiConfig = method.invoke(wifiManager)
            wifiConfig?.let {
                val ssidField = it.javaClass.getDeclaredField("SSID")
                ssidField.isAccessible = true
                (ssidField.get(it) as? String)?.trim('"')
            }
        } catch (e: Exception) {
            null
        }
    }


}