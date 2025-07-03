package org.horizontal.tella.mobile.data.peertopeer

import android.util.Log
import com.hzontal.tella_vault.VaultFile
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.remote.PeerApiRoutes
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadResult
import org.horizontal.tella.mobile.domain.peertopeer.P2PFile
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.horizontal.tella.mobile.util.FileUtil.getMimeType
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerSessionManager
import org.json.JSONObject
import timber.log.Timber
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
        pin: String,
    ): Result<String> = withContext(Dispatchers.IO) {

        val url = PeerApiRoutes.buildUrl(ip, port, PeerApiRoutes.REGISTER)

        Log.d("PeerClient", "Connecting to: $url")

        try {
            val payload = PeerRegisterPayload(
                pin = pin,
                nonce = UUID.randomUUID().toString(),
             //   autoUpload
            )

            val jsonPayload = Json.encodeToString(payload)
            val requestBody = jsonPayload.toRequestBody()

            val client = getClientWithFingerprintValidation(expectedFingerprint)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body.string()
                    TODO("WE SHOULD USE TIMBER INSTEAD")
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
    ): PrepareUploadResult = withContext(Dispatchers.IO) {

        val url = PeerApiRoutes.buildUrl(ip, port, PeerApiRoutes.PREPARE_UPLOAD)

        val fileItems = files.map {
            val mimeType = getMimeType(it.name) ?: "application/octet-stream"
            P2PFile(
                id = it.id,
                fileName = it.name,
                size = it.size,
                fileType = mimeType,
                sha256 = it.hash
            )
        }

        val requestPayload = PrepareUploadRequest(title, sessionId, fileItems)
        val jsonPayload = Json.encodeToString(requestPayload)
        val requestBody = jsonPayload.toRequestBody()
        val client = getClientWithFingerprintValidation(expectedFingerprint)

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    return@withContext try {
                        val transmissionId = JSONObject(body).getString("transmissionId")
                        // Store it in the session manager
                        PeerSessionManager.setTransmissionId(transmissionId)
                        PrepareUploadResult.Success(transmissionId)
                    } catch (e: Exception) {
                        Log.e("PrepareUpload", "Invalid JSON response: $body", e)
                        PrepareUploadResult.Failure(Exception("Malformed server response"))
                    }
                } else {
                    Log.e("PrepareUpload", "Server error ${response.code}: ${response.message}")
                    return@withContext when (response.code) {
                        400 -> PrepareUploadResult.BadRequest
                        403 -> PrepareUploadResult.Forbidden
                        409 -> PrepareUploadResult.Conflict
                        500 -> PrepareUploadResult.ServerError
                        else -> PrepareUploadResult.Failure(Exception("Unhandled server error ${response.code}"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PrepareUpload", "Exception during upload: ${e.message}", e)
            PrepareUploadResult.Failure(e)
        }
    }


}