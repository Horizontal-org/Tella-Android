package org.horizontal.tella.mobile.data.peertopeer

import android.util.Log
import com.hzontal.tella_vault.VaultFile
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.domain.peertopeer.P2PFile
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class TellaPeerToPeerClient {

    private fun getClientWithFingerprintValidation(expectedFingerprint: String): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String?) {}

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String?) {
                val serverCert = chain[0]
                val actualFingerprint = CertificateUtils.getPublicKeyHash(serverCert)

                if (!actualFingerprint.equals(expectedFingerprint, ignoreCase = true)) {
                    throw CertificateException(
                        "Server certificate fingerprint mismatch.\nExpected: $expectedFingerprint\nGot: $actualFingerprint"
                    )
                }
            }
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    suspend fun registerPeerDevice(
        ip: String,
        port: String,
        expectedFingerprint: String,
        pin: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val url = "https://$ip:$port/api/v1/register"
        Log.d("PeerClient", "Connecting to: $url")

        try {
            val payload = PeerRegisterPayload(
                pin = pin,
                nonce = UUID.randomUUID().toString()
            )

            val jsonPayload = Json.encodeToString(payload)
            val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

            val client = getClientWithFingerprintValidation(expectedFingerprint)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    Log.e(
                        "PeerClient", """
                    HTTP ${response.code} Error
                    URL: $url
                    Headers: ${response.headers}
                    Body: $errorBody
                """.trimIndent()
                    )
                    return@use Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }

                val contentType = response.header("Content-Type") ?: ""
                if (!contentType.contains("application/json")) {
                    Log.w("PeerClient", "Unexpected Content-Type: $contentType")
                }

                val body = response.body?.string() ?: ""
                return@use try {
                    val json = JSONObject(body)
                    Result.success(json.getString("sessionId"))
                } catch (e: Exception) {
                    Result.failure(Exception("Invalid JSON response: ${e.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e("PeerClient", "Request failed", e)
            Result.failure(e)
        }
    }

    suspend fun prepareUpload(
        ip: String,
        port: String,
        expectedFingerprint: String,
        title: String,
        files: List<VaultFile>,
        sessionId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val url = "https://$ip:$port/api/v1/prepare-upload"

        val fileItems = files.map {
            P2PFile(
                id = it.id,
                fileName = it.name,
                size = it.size,
                fileType = "application/octet-stream",
                sha256 = it.hash
            )
        }

        val requestPayload = PrepareUploadRequest(title, sessionId, fileItems)
        val jsonPayload = Json.encodeToString(requestPayload)
        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
        val client = getClientWithFingerprintValidation(expectedFingerprint)

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            return@withContext client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    try {
                        val transmissionId = JSONObject(body).getString("transmissionId")
                        Result.success(transmissionId)
                    } catch (e: Exception) {
                        Log.e("PrepareUpload", "Invalid JSON response: $body", e)
                        Result.failure(Exception("Malformed server response"))
                    }
                } else {
                    Log.e("PrepareUpload", "Server error ${response.code}: ${response.message}")
                    when (response.code) {
                        400 -> Log.e("PrepareUpload", "Bad Request – likely missing or invalid fields.")
                        403 -> Log.e("PrepareUpload", "Forbidden – the server is refusing the request.")
                        409 -> Log.e("PrepareUpload", "Conflict – maybe another active session.")
                        500 -> Log.e("PrepareUpload", "Internal Server Error – try again later.")
                        else -> Log.e("PrepareUpload", "Unhandled server error code.")
                    }
                    Result.failure(Exception("Server returned error ${response.code}"))
                }
            }

        } catch (e: Exception) {
            Log.e("PrepareUpload", "Exception during upload: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun fetchServerFingerprint(ip: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val socket = SSLSocketFactory.getDefault()
                .createSocket(ip, port) as SSLSocket
            socket.soTimeout = 5000
            socket.startHandshake()

            val cert = socket.session.peerCertificates[0] as X509Certificate
            val fingerprint = CertificateUtils.getPublicKeyHash(cert)
            Result.success(fingerprint)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



}