package org.horizontal.tella.mobile.data.peertopeer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.certificate.CertificateUtils
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object FingerprintFetcher {

    suspend fun fetch(context: Context, ip: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            createBoundSSLSocket(context, ip, port).use { socket ->
                val cert = socket.session.peerCertificates[0] as X509Certificate
                val fingerprint = CertificateUtils.getPublicKeyHash(cert)
                Result.success(fingerprint)
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(RuntimeException("Connection timed out", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createBoundSSLSocket(context: Context, ip: String, port: Int): SSLSocket {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(TrustAllCerts()), SecureRandom())
        val factory = sslContext.socketFactory as SSLSocketFactory

        // Create plain socket
        val plainSocket: Socket = factory.createSocket() as SSLSocket
        plainSocket.soTimeout = 5000

        // Bind to Wi-Fi network if available
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.allNetworks.firstOrNull { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }?.bindSocket(plainSocket)

        // Connect and handshake
        plainSocket.connect(java.net.InetSocketAddress(ip, port), 5000)
        (plainSocket as SSLSocket).startHandshake()

        return plainSocket
    }

    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
}
