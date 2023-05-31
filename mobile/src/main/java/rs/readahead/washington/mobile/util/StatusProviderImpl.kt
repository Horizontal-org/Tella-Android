package rs.readahead.washington.mobile.util

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
}


