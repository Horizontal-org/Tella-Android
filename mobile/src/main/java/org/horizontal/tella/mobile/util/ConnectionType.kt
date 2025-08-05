package org.horizontal.tella.mobile.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.net.Inet4Address
import java.net.NetworkInterface

enum class ConnectionType {
    WIFI, CELLULAR, HOTSPOT, NONE
}

data class NetworkInfo(
    val connectionType: ConnectionType,
    val ssid: String?,
    val ipAddress: String?,
    var port: String = "53317"
)

class NetworkInfoManager(private val context: Context) {

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo

    private var currentNetworkInfo: NetworkInfo = NetworkInfo(ConnectionType.NONE, null, null)

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission", "DiscouragedPrivateApi")
    fun fetchCurrentNetworkInfo() {
        val connectivityManager =
            ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)

        when {
            capabilities == null -> {
                _networkInfo.postValue(NetworkInfo(ConnectionType.NONE, null, null))
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

                val wifiInfo = wifiManager?.connectionInfo
                val ssid = when {
                    wifiInfo?.ssid != null && wifiInfo.ssid != WifiManager.UNKNOWN_SSID ->
                        wifiInfo.ssid.trim('"')
                    else -> "Unknown WiFi"
                }

                val ipAddress = getLocalIpAddress()
                _networkInfo.postValue(NetworkInfo(ConnectionType.WIFI, ssid, ipAddress))
            }

            isDeviceHotspotEnabled(context) -> {
                val hotspotSSID = getDeviceHotspotSSID(context) ?: "Hotspot"
                val hotspotIpAddress = getLocalIpAddress() // more reliable than hardcoded
                _networkInfo.postValue(NetworkInfo(ConnectionType.HOTSPOT, hotspotSSID, hotspotIpAddress))
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                _networkInfo.postValue(NetworkInfo(ConnectionType.CELLULAR, null, null))
            }

            else -> {
                _networkInfo.postValue(NetworkInfo(ConnectionType.NONE, null, null))
            }
        }

        currentNetworkInfo = _networkInfo.value ?: NetworkInfo(ConnectionType.NONE, null, null)
    }

    private fun isDeviceHotspotEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as? Boolean ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getDeviceHotspotSSID(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("getWifiApConfiguration")
            method.isAccessible = true
            val config = method.invoke(wifiManager)
            val ssidField = config?.javaClass?.getDeclaredField("SSID")
            ssidField?.isAccessible = true
            ssidField?.get(config) as? String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
