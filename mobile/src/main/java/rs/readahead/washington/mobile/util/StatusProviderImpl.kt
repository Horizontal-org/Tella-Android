package rs.readahead.washington.mobile.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build

/**
 *
 * This class implements [StatusProvider] and provides configurations required on runtime.
 *
 * @constructor Creates a ConfigProvider using the Android Application context.
 * @param context the Android Application context.
 */
class StatusProviderImpl(val context: Context) : StatusProvider {

    /**
     * This function uses [ConnectivityManager]  to check active Network Capabilities to know
     * the Android phone network connectivity status.
     *
     * @return is the phone connected to a network through cellular or WiFi.
     */
    @Suppress("ReturnCount")
    override fun isOnline(): Boolean {
        val cm: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        if (Build.VERSION.SDK_INT < 23) {
            val ni = cm.activeNetworkInfo

            if (ni != null) {
                return ni.isConnected && (ni.type == ConnectivityManager.TYPE_WIFI || ni.type == ConnectivityManager.TYPE_MOBILE)
            }
        } else {
            val activeNetwork: Network = cm.activeNetwork ?: return false

            val networkCapabilities: NetworkCapabilities =
                cm.getNetworkCapabilities(activeNetwork) ?: return false

            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }

        return false
    }
}
