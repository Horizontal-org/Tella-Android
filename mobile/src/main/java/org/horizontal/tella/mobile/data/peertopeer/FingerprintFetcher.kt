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
import okhttp3.CertificatePinner
import okhttp3.Dns
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume

/**
 * Fingerprint payloads:
 * - spkiHex : SHA-256(SPKI) in lowercase hex (optional, useful for logs)
 * - okHttpPin: OkHttp SPKI pin string "sha256/<base64>" (optional, if you want SPKI pinning)
 * - certHex : SHA-256(leaf certificate DER) in lowercase hex (matches iOS behavior)
 */
data class FingerprintResult(
    val spkiHex: String,
    val okHttpPin: String,
    val certHex: String
)

object FingerprintFetcher {

    // ---------------------------------------------------------------------
    // Public: PRE-PIN handshake to read cert (no HTTP), then compute hashes
    // ---------------------------------------------------------------------
    suspend fun fetch(context: Context, ip: String, port: Int): kotlin.Result<FingerprintResult> =
        withContext(Dispatchers.IO) {
            try {
                val wifi = getWifiNetworkPreferringValidated(context)

                // 1) Quick TCP probe (fail fast if nothing listening)
                probeTcp(ip, port, wifi)

                // 2) TLS handshake (trust-all) to read leaf cert
                createBoundTlsSocket(ip, port, wifi).use { tls ->
                    val cert = tls.session.peerCertificates.first() as X509Certificate
                    return@withContext kotlin.Result.success(fingerprintFromCert(cert))
                }
            } catch (e: SSLHandshakeException) {
                val msg = e.message.orEmpty()
                return@withContext if (
                    msg.contains("unexpected_message", true) ||
                    msg.contains("unrecognized_name", true) ||
                    msg.contains("EOF", true) ||
                    msg.contains("received fatal alert", true)
                ) {
                    kotlin.Result.failure(
                        IllegalStateException("Server on $port appears to be plain HTTP or misconfigured TLS. ${e.message}")
                    )
                } else {
                    kotlin.Result.failure(e)
                }
            } catch (e: SocketTimeoutException) {
                kotlin.Result.failure(RuntimeException("Connection timed out to $ip:$port", e))
            } catch (e: IOException) {
                kotlin.Result.failure(IOException("I/O error to $ip:$port: ${e.message}", e))
            } catch (e: Throwable) {
                kotlin.Result.failure(e)
            }
        }

    // ---------------------------------------------------------------------
    // Public: Build clients that ENFORCE identity
    //   A) By certificate DER hash (matches iOS) -> Interceptor based
    //   B) By SPKI pin (OkHttp native) -> Optional
    // ---------------------------------------------------------------------

    fun buildClientPinnedByCertHash(
        expectedCertSha256Hex: String,
        hostForRequests: String,
        network: Network? = null
    ): OkHttpClient {
        val trustAll: X509TrustManager = TrustAllCerts()
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustAll), SecureRandom())
        }

        val builder = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true } // connect by IP; identity enforced by our hash check
            .addNetworkInterceptor(LeafCertHashInterceptor(expectedCertSha256Hex))
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .writeTimeout(7, TimeUnit.SECONDS)

        if (network != null) {
            builder.socketFactory(network.socketFactory)
            builder.dns(Dns { hostname -> network.getAllByName(hostname).toList() })
        }
        return builder.build()
    }

    suspend fun buildClientPinnedByCertHash(
        context: Context,
        expectedCertSha256Hex: String,
        hostForRequests: String
    ): OkHttpClient {
        val wifi = getWifiNetworkPreferringValidated(context)
        return buildClientPinnedByCertHash(expectedCertSha256Hex, hostForRequests, wifi)
    }

    // Optional: SPKI pinning (OkHttp-native). Keep if you want SPKI instead of DER hash.
    fun buildPinnedClientWithOkPin(
        okHttpPin: String,              // must be "sha256/<base64>"
        hostForPin: String,             // same host used in the URL (IP or DNS)
        network: Network? = null
    ): OkHttpClient {
        require(okHttpPin.startsWith("sha256/")) { "Pin must start with 'sha256/'" }

        val pinner = CertificatePinner.Builder()
            .add(hostForPin, okHttpPin)
            .build()

        val trustAll: X509TrustManager = TrustAllCerts()
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustAll), SecureRandom())
        }

        val builder = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true } // connect by IP; identity enforced by pin
            .certificatePinner(pinner)
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .writeTimeout(7, TimeUnit.SECONDS)

        if (network != null) {
            builder.socketFactory(network.socketFactory)
            builder.dns(Dns { hostname -> network.getAllByName(hostname).toList() })
        }
        return builder.build()
    }

    suspend fun buildPinnedClientWithOkPin(
        context: Context,
        okHttpPin: String,
        hostForPin: String
    ): OkHttpClient {
        val wifi = getWifiNetworkPreferringValidated(context)
        return buildPinnedClientWithOkPin(okHttpPin, hostForPin, wifi)
    }

    // ---------------------------------------------------------------------
    // Public helpers
    // ---------------------------------------------------------------------

    suspend fun pickWifiNetwork(context: Context): Network? =
        getWifiNetworkPreferringValidated(context)

    fun fingerprintFromCert(cert: X509Certificate): FingerprintResult {
        val okHttpPin = CertificatePinner.pin(cert)           // "sha256/<base64>" of SPKI
        val spkiHex   = sha256Hex(cert.publicKey.encoded)     // SPKI hex (optional)
        val certHex   = sha256Hex(cert.encoded)               // ✅ CERT DER hex (matches iOS)
        return FingerprintResult(spkiHex = spkiHex, okHttpPin = okHttpPin, certHex = certHex)
    }

    // ---------------------------------------------------------------------
    // Internals used by fetch()
    // ---------------------------------------------------------------------

    private fun createBoundTlsSocket(ip: String, port: Int, wifi: Network?): SSLSocket {
        val sslContext = SSLContext.getInstance("TLS").apply {
            // Trust-all only to read the cert; real requests will be pinned
            init(null, arrayOf<TrustManager>(TrustAllCerts()), SecureRandom())
        }
        val factory = sslContext.socketFactory as SSLSocketFactory

        val s = factory.createSocket() as SSLSocket
        s.soTimeout = 7000
        s.enabledProtocols = arrayOf("TLSv1.3", "TLSv1.2")

        try {
            wifi?.bindSocket(s)
        } catch (bindErr: Exception) {
            Timber.w(bindErr, "bindSocket failed, continuing on default route")
        }

        s.connect(InetSocketAddress(ip, port), 7000)
        s.startHandshake()
        return s
    }

    private fun probeTcp(ip: String, port: Int, wifi: Network?) {
        val sock = Socket()
        try { wifi?.bindSocket(sock) } catch (_: Exception) {}
        try { sock.connect(InetSocketAddress(ip, port), 2500) } finally {
            try { sock.close() } catch (_: Exception) {}
        }
    }

    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    @Suppress("DEPRECATION")
    private suspend fun getWifiNetworkPreferringValidated(ctx: Context): Network? =
        withContext(Dispatchers.IO) {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Fast path: active VALIDATED Wi-Fi
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
                cm.allNetworks.firstOrNull { n ->
                    val info = cm.getNetworkInfo(n)
                    val caps = cm.getNetworkCapabilities(n)
                    info?.isConnected == true &&
                            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
                            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }?.let { return@withContext it }
            }

            // Request a validated Wi-Fi (time-bound)
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
                            cm.requestNetwork(request, cb)
                        }
                        cont.invokeOnCancellation { runCatching { cm.unregisterNetworkCallback(cb) } }
                    }
                }
            } catch (_: Exception) { null }

            if (validated != null) return@withContext validated

            // Last resort: any connected Wi-Fi with INTERNET
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.allNetworks.firstOrNull { n ->
                    cm.getNetworkCapabilities(n)?.let { c ->
                        c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                                c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    } == true
                }
            } else null
        }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    // Interceptor to enforce SHA-256 over the LEAF CERTIFICATE (DER), iOS-compatible
    private class LeafCertHashInterceptor(
        private val expectedHexLower: String
    ) : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val resp = chain.proceed(chain.request())
            val cert = (resp.handshake?.peerCertificates?.firstOrNull() as? X509Certificate)
                ?: throw javax.net.ssl.SSLPeerUnverifiedException("No peer certificate")
            val actual = sha256Hex(cert.encoded)
            if (!actual.equals(expectedHexLower, ignoreCase = true)) {
                resp.close()
                throw javax.net.ssl.SSLPeerUnverifiedException(
                    "Certificate DER hash mismatch. expected=$expectedHexLower actual=$actual"
                )
            }
            return resp
        }
    }
}
