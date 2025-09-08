package org.horizontal.tella.mobile.data.peertopeer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.horizontal.tella.mobile.certificate.CertificateUtils
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume

object FingerprintFetcher {

    /**
     * Connects via TLS over (preferably) validated Wi-Fi, returns SPKI SHA-256 hex (lowercase).
     * Produces descriptive failures (HTTP vs TLS, route, timeout).
     */
    suspend fun fetch(context: Context, ip: String, port: Int): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // 0) Pick a Wi-Fi network (validated if possible)
                val wifi = getWifiNetworkPreferringValidated(context)
                if (wifi != null) {
                    Timber.d("FingerprintFetcher using Wi-Fi network: $wifi")
                } else {
                    Timber.w("FingerprintFetcher: no Wi-Fi network bound (falling back to default route)")
                }

                // 1) Quick TCP probe – fail fast if nothing is listening
                probeTcp(ip, port, wifi)

                // 2) TLS handshake to read cert
                createBoundTlsSocket(ip, port, wifi).use { tls ->
                    val cert = tls.session.peerCertificates.first() as X509Certificate
                    val fp = CertificateUtils.getPublicKeyHash(cert).lowercase()
                    return@withContext Result.success(fp)
                }
            } catch (e: SSLHandshakeException) {
                // Common when the server speaks plain HTTP on this port
                val msg = e.message.orEmpty()
                return@withContext if (
                    msg.contains("unexpected_message", true) ||
                    msg.contains("unrecognized_name", true) ||
                    msg.contains("EOF", true) ||
                    msg.contains("received fatal alert", true)
                ) {
                    Result.failure(IllegalStateException("Server on $port appears to be plain HTTP or wrong TLS config (plain HTTP on TLS port). ${e.message}"))
                } else {
                    Result.failure(e)
                }
            } catch (e: SocketTimeoutException) {
                return@withContext Result.failure(RuntimeException("Connection timed out to $ip:$port", e))
            } catch (e: IOException) {
                return@withContext Result.failure(IOException("I/O error to $ip:$port: ${e.message}", e))
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

    // ---- Internals ----

    private fun createBoundTlsSocket(ip: String, port: Int, wifi: Network?): SSLSocket {
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(TrustAllCerts()), SecureRandom())
        }
        val factory = sslContext.socketFactory as SSLSocketFactory

        val s = factory.createSocket() as SSLSocket
        s.soTimeout = 7000
        // Offer both; server chooses
        s.enabledProtocols = arrayOf("TLSv1.3", "TLSv1.2")

        // Bind to selected Wi-Fi path if we have one
        try {
            wifi?.bindSocket(s)
        } catch (bindErr: Exception) {
            Timber.w(bindErr, "bindSocket failed, continuing on default route")
        }

        s.connect(InetSocketAddress(ip, port), 7000)
        s.startHandshake()
        return s
    }

    /**
     * Short TCP probe to distinguish "port closed / no route" from TLS errors.
     */
    private fun probeTcp(ip: String, port: Int, wifi: Network?) {
        val sock = Socket()
        try {
            wifi?.bindSocket(sock)
        } catch (_: Exception) {
        }
        try {
            sock.connect(InetSocketAddress(ip, port), 2500)
        } finally {
            try { sock.close() } catch (_: Exception) {}
        }
    }

    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    /**
     * Prefer a VALIDATED Wi-Fi network; if not delivered within ~4–5s,
     * fallback to a connected Wi-Fi with INTERNET capability.
     */
    @Suppress("DEPRECATION")
    private suspend fun getWifiNetworkPreferringValidated(ctx: Context): Network? =
        withContext(Dispatchers.IO) {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Fast path: active VALIDATED Wi-Fi (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.activeNetwork?.let { n ->
                    cm.getNetworkCapabilities(n)?.let { c ->
                        if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                            c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        ) return@withContext n
                    }
                }
            } else {
                // API 21–22: pick any connected Wi-Fi with INTERNET
                cm.allNetworks.firstOrNull { n ->
                    val info = cm.getNetworkInfo(n)
                    val caps = cm.getNetworkCapabilities(n)
                    info?.isConnected == true &&
                            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
                            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }?.let { return@withContext it }
            }

            // Try to request a validated Wi-Fi, but time-bound
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val validated: Network? = try {
                withTimeout(4_000) {
                    suspendCancellableCoroutine { cont ->
                        val cb = object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: Network) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                    if (cont.isActive) {
                                        cm.unregisterNetworkCallback(this)
                                        cont.resume(network)
                                    }
                                }
                            }
                            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                                ) {
                                    if (cont.isActive) {
                                        cm.unregisterNetworkCallback(this)
                                        cont.resume(network)
                                    }
                                }
                            }
                            override fun onUnavailable() {
                                if (cont.isActive) {
                                    cm.unregisterNetworkCallback(this)
                                    cont.resume(null)
                                }
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            cm.requestNetwork(request, cb, 4_000)
                        } else {
                            cm.requestNetwork(request, cb) // guarded by withTimeout
                        }
                        cont.invokeOnCancellation { runCatching { cm.unregisterNetworkCallback(cb) } }
                    }
                }
            } catch (_: Exception) { null }

            if (validated != null) return@withContext validated

            // Last resort: any connected Wi-Fi with INTERNET (even if not VALIDATED yet)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.allNetworks.firstOrNull { n ->
                    cm.getNetworkCapabilities(n)?.let { c ->
                        c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                                c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    } == true
                }
            } else {
                // Already handled above for 21–22
                null
            }
        }
}
