package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
class PeerToPeerViewModel  @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo

    @RequiresApi(Build.VERSION_CODES.M)
    fun fetchCurrentNetworkInfo() {
        val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)

        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)

        when {
            capabilities == null -> {
                _networkInfo.value = NetworkInfo(ConnectionType.NONE, null, null)
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo

                val ssid = wifiInfo.ssid.trim('"')  // Removes quotes from SSID
                val ipAddress = Formatter.formatIpAddress(wifiInfo.ipAddress)

                _networkInfo.value = NetworkInfo(ConnectionType.WIFI, ssid, ipAddress)
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                val networkOperatorName = telephonyManager.networkOperatorName

                _networkInfo.value = NetworkInfo(ConnectionType.CELLULAR, networkOperatorName, null)
            }

            else -> {
                _networkInfo.value = NetworkInfo(ConnectionType.NONE, null, null)
            }
        }
    }

}