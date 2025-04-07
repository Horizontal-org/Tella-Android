package org.horizontal.tella.mobile.views.fragment.peertopeer

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.ConnectionType
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.NetworkInfo
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@HiltViewModel
class PeerToPeerViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo
    var currentNetworkInfo: NetworkInfo? = null
        private set

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

    @SuppressLint("DiscouragedPrivateApi")
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

    fun onQrCodeParsed(ip: String, port: String, hash: String, pin: String) {
        Log.d("QRCode", "Connecting to $ip:$port with hash $hash")
        viewModelScope.launch {
            register(ip, port, hash, pin)
        }
    }

    private fun getClientWithFingerprintValidation(
        expectedFingerprint: String
    ): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String?) {}

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String?) {
                val serverCert = chain[0]
                val actualFingerprint = CertificateUtils.getPublicKeyHash(serverCert)

                if (!actualFingerprint.equals(expectedFingerprint, ignoreCase = true)) {
                    throw CertificateException("Server certificate fingerprint mismatch.\nExpected: $expectedFingerprint\nGot: $actualFingerprint")
                }
            }
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // Set connect timeout
            .readTimeout(30, TimeUnit.SECONDS)     // Set read timeout
            .writeTimeout(30, TimeUnit.SECONDS)    // Set write timeout
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }


    // Function to send a registration POST request to a peer device over HTTPS
    private  fun register(ip: String, port: String, expectedFingerprint: String, pin: String)
    {
        val url = "https://$ip:$port/api/register"

        val alias = "Device_${Build.MODEL.replace(" ", "_")}"
        val version = "2.0"
        val deviceModel = Build.MODEL
        val deviceType = "mobile"
        val protocol = "https"
        val download = true
        val localPort = 53317

        val jsonPayload = """
        {
            "alias": "$alias",
            "version": "$version",
            "deviceModel": "$deviceModel",
            "deviceType": "$deviceType",
            "fingerprint": "$expectedFingerprint",
            "port": $localPort,
            "protocol": "$protocol",
            "download": $download,
            "pin": "$pin",
            "nonce": "${UUID.randomUUID()}"
        }
    """.trimIndent()

        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

        val client = getClientWithFingerprintValidation(expectedFingerprint)

        viewModelScope.launch {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("Register", "Success: ${response.body?.string()}")
                } else {
                    Log.e("Register", "Failed: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("Register", "Error: ${e.message}", e)
            }
        }
    }


}