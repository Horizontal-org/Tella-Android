package org.horizontal.tella.mobile.data.peertopeer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object ServerPinger {

    suspend fun pingAndExtractFingerprint(
        context: Context,
        ip: String,
        port: Int
    ): Result<FingerprintResult> = withContext(Dispatchers.IO) {
        try {
            val network = FingerprintFetcher.pickWifiNetwork(context)

            val trustAll: X509TrustManager = FingerprintFetcher.TrustAllCerts()
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustAll), SecureRandom())
            }

            val builder = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAll)
                .hostnameVerifier { _, _ -> true } // connect by IP
                .connectTimeout(7, TimeUnit.SECONDS)
                .readTimeout(7, TimeUnit.SECONDS)
                .writeTimeout(7, TimeUnit.SECONDS)

            if (network != null) {
                builder.socketFactory(network.socketFactory)
                builder.dns(Dns { hostname -> network.getAllByName(hostname).toList() })
            }

            val client = builder.build()

            val req = Request.Builder()
                .url("https://$ip:$port/api/v1/ping")
                .post(RequestBody.create(null, ByteArray(0)))
                .build()

            client.newCall(req).execute().use { resp ->
                val cert = (resp.handshake?.peerCertificates?.firstOrNull()
                    ?: return@withContext Result.failure(IllegalStateException("No peer certificate in handshake"))
                        ) as X509Certificate

                val fp = FingerprintFetcher.fingerprintFromCert(cert)
                // Even if HTTP code isn’t 2xx, we already got the cert.
                return@withContext Result.success(fp)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Pinned ping using CERT DER hash (matches iOS).
     */
    suspend fun notifyServerPinnedByCert(
        context: Context,
        ip: String,
        port: Int,
        expectedCertSha256Hex: String
    ) = withContext(Dispatchers.IO) {
        val client = FingerprintFetcher.buildClientPinnedByCertHash(
            context = context,
            expectedCertSha256Hex = expectedCertSha256Hex,
            hostForRequests = ip
        )

        val req = Request.Builder()
            .url("https://$ip:$port/api/v1/ping")
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Ping failed: HTTP ${resp.code}")
        }
    }

    /**
     * Optional: pinned ping using SPKI pin (OkHttp-native).
     */
    suspend fun notifyServerPinnedWithOkPin(
        context: Context,
        ip: String,
        port: Int,
        okHttpPin: String
    ) = withContext(Dispatchers.IO) {
        val client = FingerprintFetcher.buildPinnedClientWithOkPin(
            context = context,
            okHttpPin = okHttpPin,
            hostForPin = ip
        )

        val req = Request.Builder()
            .url("https://$ip:$port/api/v1/ping")
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Ping failed: HTTP ${resp.code}")
        }
    }
}
