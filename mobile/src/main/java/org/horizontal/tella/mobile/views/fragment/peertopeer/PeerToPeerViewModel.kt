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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerClient
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.ConnectionType
import org.horizontal.tella.mobile.views.fragment.peertopeer.data.NetworkInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
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
class PeerToPeerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerClient: TellaPeerToPeerClient
) :
    ViewModel() {

    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo
    var currentNetworkInfo: NetworkInfo? = null
    private val _registrationSuccess = MutableLiveData<Boolean>()
    val registrationSuccess: LiveData<Boolean> get() = _registrationSuccess

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
        viewModelScope.launch {
            val result = peerClient.registerPeerDevice(ip, port, hash, pin)
            result.onSuccess {
                Log.d("QRCode", "Registered successfully: $it")
                val file = createFileFromAsset(context, "testDemo.txt")
                _registrationSuccess.postValue(true) // Notify success
            }.onFailure {
                //   peerClient.prepareUpload(ip,port,hash,"test report",file,UUID.randomUUID().toString(),calculateSha256(file),UUID.randomUUID().toString())
                // update UI state
                Log.e("QRCode", "Registration failed: ${it.message}")
                _registrationSuccess.postValue(false) // Optional: Notify failure
            }
        }
    }


    private fun createFileFromAsset(context: Context, assetFileName: String): File {
        // Create a temporary file in the cache directory
        val tempFile = File(context.cacheDir, assetFileName)

        if (!tempFile.exists()) {
            Log.e("PrepareUpload", "File does not exist: ${tempFile.absolutePath}")
        }

        // Open the asset file
        context.assets.open(assetFileName).use { inputStream ->
            // Write the content of the asset to the temporary file
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return tempFile
    }



    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }



}