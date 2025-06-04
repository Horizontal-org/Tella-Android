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
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.data.peertopeer.FingerprintFetcher
import org.horizontal.tella.mobile.data.peertopeer.PrepareUploadRequest
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.ServerPinger
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.ConnectionType
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.NetworkInfo
import timber.log.Timber
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PeerToPeerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerClient: TellaPeerToPeerClient,
    private val peerToPeerManager: PeerToPeerManager
) : ViewModel() {

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo
    var currentNetworkInfo: NetworkInfo? = null
    private val _registrationSuccess = MutableLiveData<Boolean>()
    val registrationSuccess: LiveData<Boolean> get() = _registrationSuccess
    private val _getHashSuccess = MutableLiveData<String>()
    val getHashSuccess: LiveData<String> get() = _getHashSuccess
    private val _getHashError = MutableLiveData<Throwable>()
    val getHashError: LiveData<Throwable> get() = _getHashError
    private val _sessionInfo = MutableStateFlow<PeerConnectionInfo?>(null)
    val sessionInfo: StateFlow<PeerConnectionInfo?> = _sessionInfo
    private val _registrationServerSuccess = MutableLiveData<Boolean>()
    val registrationServerSuccess: LiveData<Boolean> = _registrationServerSuccess
    private val _incomingUploadRequest = MutableLiveData<PrepareUploadRequest>()
    val incomingUploadRequest: LiveData<PrepareUploadRequest> = _incomingUploadRequest

    val clientHash = peerToPeerManager.clientConnected


//    init {
//        viewModelScope.launch {
//            PeerEventManager.incomingUploadRequests.collect { request ->
//                _incomingUploadRequest.postValue(request)
//            }
//        }
//    }

    init {
        viewModelScope.launch {
            PeerEventManager.registrationEvents.collect { success ->
                _registrationServerSuccess.postValue(success)
            }
        }
    }
    fun resetRegistrationState() {
        _registrationServerSuccess.postValue(false)
    }
    @RequiresApi(Build.VERSION_CODES.M)
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

            isDeviceHotspotEnabled(context) -> {
                val hotspotSSID = getDeviceHotspotSSID(context) ?: "Hotspot"
                val hotspotIpAddress = getDeviceHotspotIpAddress()
                _networkInfo.value =
                    NetworkInfo(ConnectionType.HOTSPOT, hotspotSSID, hotspotIpAddress)
            }

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                _networkInfo.value = NetworkInfo(ConnectionType.CELLULAR, null, null)
            }

            else -> {
                _networkInfo.value = NetworkInfo(ConnectionType.NONE, null, null)
            }
        }


        currentNetworkInfo = _networkInfo.value
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

    @SuppressLint("DiscouragedPrivateApi")
    private fun isDeviceHotspotEnabled(context: Context): Boolean {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    private fun getWifiSsidFromCapabilities(capabilities: NetworkCapabilities): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (capabilities.transportInfo as? WifiInfo)?.ssid?.trim('"')
        } else {
            null
        }
    }

    private fun getDeviceHotspotIpAddress(): String? {
        return try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            networkInterfaces.iterator().forEach { networkInterface ->
                networkInterface.inetAddresses.iterator().forEach { inetAddress ->
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        if (networkInterface.displayName.contains("wlan") || networkInterface.displayName.contains(
                                "ap"
                            )
                        ) {
                            return inetAddress.hostAddress
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun startRegistration(ip: String, port: String, hash: String, pin: String) {
        viewModelScope.launch {
            val result = peerClient.registerPeerDevice(ip, port, hash, pin)
            result.onSuccess { sessionId ->
                PeerSessionManager.saveConnectionInfo(ip, port, hash, sessionId)
                // update UI state
                _registrationSuccess.postValue(true)
            }.onFailure { error ->
                Timber.d("error ***** $error")

            }
        }
    }

    fun handleCertificate(ip: String, port: String, pin: String) {
        viewModelScope.launch {
            val result = FingerprintFetcher.fetch(ip, port.toInt())
            result.onSuccess { hash ->
                Timber.d("hash ***** $hash")
                _getHashSuccess.postValue(hash)

                // Notify the server after fetching the hash
                runCatching {
                    ServerPinger.notifyServer(ip, port.toInt())
                }.onFailure {
                    Timber.e(it, "Failed to ping server after fetching hash")
                }

            }.onFailure { error ->
                Timber.d("error ***** $error")
                _getHashError.postValue(error)
            }
        }
    }


    fun setPeerSessionInfo(info: PeerConnectionInfo) {
        _sessionInfo.value = info
    }
}