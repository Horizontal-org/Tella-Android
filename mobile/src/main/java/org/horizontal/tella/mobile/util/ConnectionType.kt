package org.horizontal.tella.mobile.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
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
    val ipAddress: String?,
    var port: String = "53317"
)

class NetworkInfoManager(private val context: Context) {

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo

    private var currentNetworkInfo: NetworkInfo = NetworkInfo(ConnectionType.NONE, null)

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    fun fetchCurrentNetworkInfo() {
        val cm = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        val network: Network? = cm?.activeNetwork
        val caps: NetworkCapabilities? = cm?.getNetworkCapabilities(network)

        if (caps == null) {
            post(ConnectionType.NONE, null)
            return
        }

        val ip = getActiveIpv4(cm, network) ?: getFallbackIpv4()

        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                post(ConnectionType.WIFI, ip)
            }

            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (isDeviceHotspotEnabled(context)) {
                    post(ConnectionType.HOTSPOT, ip) // treat hotspot + cellular as HOTSPOT
                } else {
                    post(ConnectionType.NONE, null)   // cellular alone → NONE
                }
            }

            else -> {
                post(ConnectionType.NONE, null)
            }
        }
    }

    private fun post(type: ConnectionType, ip: String?) {
        val info = NetworkInfo(type, ip)
        _networkInfo.postValue(info)
        currentNetworkInfo = info
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getActiveIpv4(cm: ConnectivityManager?, network: Network?): String? {
        if (cm == null || network == null) return null
        val lp: LinkProperties = cm.getLinkProperties(network) ?: return null
        return lp.linkAddresses
            .mapNotNull { it.address }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }

    private fun getFallbackIpv4(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun isDeviceHotspotEnabled(context: Context): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }
}
