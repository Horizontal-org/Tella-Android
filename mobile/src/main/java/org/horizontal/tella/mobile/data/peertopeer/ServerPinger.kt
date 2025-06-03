package org.horizontal.tella.mobile.data.peertopeer

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.security.SecureRandom
import javax.net.ssl.SSLContext

object ServerPinger {
    fun notifyServer(ip: String, port: Int) {
        val trustAllCerts = FingerprintFetcher.TrustAllCerts()
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts)
            .hostnameVerifier { _, _ -> true }
            .build()

        val request = Request.Builder()
            .url("https://$ip:$port/api/v1/ping")
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to notify server: ${response.code}")
            }
        }
    }
}
