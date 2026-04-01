package org.horizontal.tella.mobile.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build

/**
 * A StatusProvider implementation that provides network connectivity status.
 * This class requires an Android Application Context to access the Connectivity Manager.
 *
 * @property context the Android Application context.
 */
class StatusProviderImpl(private val context: Context) : StatusProvider {

    companion object {
        private const val NETWORK_TYPE_WIFI = ConnectivityManager.TYPE_WIFI
        private const val NETWORK_TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE
    }

    /**
     * Determines if the device is currently online.
     * Uses the Connectivity Manager to check if the device is currently connected
     * to a network (cellular, Wi-Fi, or Ethernet).
     *
     * @return true if the device is currently connected to a network, false otherwise.
     */
    override fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> isOnlineLegacy(cm)
            else -> isOnlineModern(cm)
        }
    }

    /**
     * For Android versions less than Marshmallow, checks if the device is currently online.
     *
     * @param cm Connectivity Manager to check network connectivity.
     * @return true if the device is currently connected to a network, false otherwise.
     */
    private fun isOnlineLegacy(cm: ConnectivityManager): Boolean {
        val ni = cm.activeNetworkInfo ?: return false
        return ni.isConnected && (ni.type == NETWORK_TYPE_WIFI || ni.type == NETWORK_TYPE_MOBILE)
    }

    /**
     * For Android versions Marshmallow and later, checks if the device is currently online.
     *
     * @param cm Connectivity Manager to check network connectivity.
     * @return true if the device is currently connected to a network, false otherwise.
     */
    @SuppressLint("NewApi")
    private fun isOnlineModern(cm: ConnectivityManager): Boolean {
        val activeNetwork: Network = cm.activeNetwork ?: return false
        val networkCapabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Determines the upstream bandwidth in Kbps.
     * Uses the Connectivity Manager to check the current connection upstream bandwidth in Kbps.
     */
    override fun upstreamBandwidthKbps(): Int? {
        val cm =  context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> upstreamBandwidthKbpsLegacy(cm)
            else -> upstreamBandwidthKbpsModern(cm)
        }
    }


    /**
     * For Android versions less than Marshmallow, determines the upstream bandwidth in Kbps.
     */
    private fun upstreamBandwidthKbpsLegacy(cm: ConnectivityManager): Int? {
        val activeInfo = cm.activeNetworkInfo ?: return null

        for (network in cm.allNetworks) {
            val info = cm.getNetworkInfo(network) ?: continue

            if (info.type == activeInfo.type && info.isConnected) {
                val caps = cm.getNetworkCapabilities(network) ?: return null
                return caps.linkUpstreamBandwidthKbps
            }
        }

        return null
    }

    /**
     * For Android versions Marshmallow and later, determines the upstream bandwidth in Kbps.
     */
    @SuppressLint("NewApi")
    private fun upstreamBandwidthKbpsModern(cm: ConnectivityManager): Int? {
        return cm.getNetworkCapabilities(cm.activeNetwork)?.linkUpstreamBandwidthKbps
    }

    /**
     * Determines if the device is currently connected to Wi-Fi.
     * Uses the Connectivity Manager to check if the device is currently connected
     * to a Wi-Fi network.
     *
     * @return true if the device is currently connected to a Wi-Fi network, false otherwise.
     */
    override fun isConnectedToWifi(): Boolean {
        val cm =  context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> isConnectedToWifiLegacy(cm)
            else -> isConnectedToWifiModern(cm)
        }
    }

    /**
     * For Android versions less than Marshmallow, checks if the device is currently connected to Wi-Fi.
     *
     * @param cm Connectivity Manager to check network connectivity.
     * @return true if the device is currently connected to a Wi-Fi network, false otherwise.
     */
    private fun isConnectedToWifiLegacy(cm: ConnectivityManager): Boolean {
        val info = cm.activeNetworkInfo ?: return false

        return info.isConnected && info.type == NETWORK_TYPE_WIFI
    }

    /**
     * For Android versions Marshmallow and later, checks if the device is currently connected to Wi-Fi.
     *
     * @param cm Connectivity Manager to check network connectivity.
     * @return true if the device is currently connected to a Wi-Fi network, false otherwise.
     */
    @SuppressLint("NewApi")
    private fun isConnectedToWifiModern(cm: ConnectivityManager): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}


