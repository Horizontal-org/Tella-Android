package org.horizontal.tella.mobile.data.peertopeer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.horizontal.tella.mobile.certificate.CertificateUtils
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object FingerprintFetcher {

    suspend fun fetch(ip: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            createUnsafeSSLSocket(ip, port).use { socket ->
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

    private fun createUnsafeSSLSocket(ip: String, port: Int): SSLSocket {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(TrustAllCerts()), SecureRandom())
        val factory = sslContext.socketFactory as SSLSocketFactory
        val socket = factory.createSocket(ip, port) as SSLSocket
        socket.soTimeout = 5000
        socket.startHandshake()
        return socket
    }

    class TrustAllCerts : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
}
