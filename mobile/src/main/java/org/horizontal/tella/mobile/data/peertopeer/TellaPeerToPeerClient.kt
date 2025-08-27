package org.horizontal.tella.mobile.data.peertopeer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

import com.hzontal.tella_vault.VaultFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants.CONTENT_TYPE
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants.CONTENT_TYPE_JSON
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants.CONTENT_TYPE_OCTET
import org.horizontal.tella.mobile.data.peertopeer.network.ProgressRequestBody
import org.horizontal.tella.mobile.data.peertopeer.remote.PeerApiRoutes
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadResult
import org.horizontal.tella.mobile.data.peertopeer.remote.RegisterPeerResult
import org.horizontal.tella.mobile.domain.peertopeer.P2PFile
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.json.JSONObject
import timber.log.Timber
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class TellaPeerToPeerClient @Inject constructor(
    @ApplicationContext private val appContext: Context
) {


    suspend fun registerPeerDevice(
        ip: String,
        port: String,
        expectedFingerprint: String, // DER SHA-256 HEX from getPublicKeyHash
        pin: String,
    ): RegisterPeerResult = withContext(Dispatchers.IO) {
        val url = PeerApiRoutes.buildUrl(ip, port, PeerApiRoutes.REGISTER)
        Timber.d("Connecting to: $url")

        val payload = PeerRegisterPayload(
            pin = pin.trim(),
            nonce = UUID.randomUUID().toString()
        )

        val jsonPayload = Json.encodeToString(payload)
        val requestBody = jsonPayload.toRequestBody()
        val client = getClientWithFingerprintValidation(ip, expectedFingerprint)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    return@use parseSessionIdFromResponse(body)
                }

                return@use when (response.code) {
                    400 -> RegisterPeerResult.InvalidFormat
                    401 -> RegisterPeerResult.InvalidPin
                    403 -> RegisterPeerResult.RejectedByReceiver
                    409 -> RegisterPeerResult.Conflict
                    429 -> RegisterPeerResult.TooManyRequests
                    500 -> RegisterPeerResult.ServerError
                    else -> RegisterPeerResult.Failure(Exception("Unhandled error ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "registerPeerDevice request failed")
            RegisterPeerResult.Failure(e)
        }
    }

    suspend fun prepareUpload(
        ip: String,
        port: String,
        expectedFingerprint: String, // DER HEX
        title: String,
        files: List<VaultFile>,
        sessionId: String
    ): PrepareUploadResult = withContext(Dispatchers.IO) {
        val url = PeerApiRoutes.buildUrl(ip, port, PeerApiRoutes.PREPARE_UPLOAD)

        val fileItems = files.map {
            val mimeType = it.mimeType ?: CONTENT_TYPE_OCTET
            P2PFile(
                id = it.id,
                fileName = it.name,
                size = it.size,
                fileType = mimeType,
                sha256 = it.hash,
                thumbnail = it.thumb
            )
        }

        val requestPayload = PrepareUploadRequest(title, sessionId, fileItems)
        val jsonPayload = Json.encodeToString(requestPayload)
        val requestBody = jsonPayload.toRequestBody()
        val client = getClientWithFingerprintValidation(ip, expectedFingerprint)

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                return@withContext if (response.isSuccessful) {
                    parseTransmissionId(responseBody)
                } else {
                    Timber.e("Server error ${response.code}: ${response.message}")
                    handleServerError(response.code, responseBody)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during prepareUpload")
            PrepareUploadResult.Failure(e)
        }
    }

    suspend fun uploadFileWithProgress(
        ip: String,
        port: String,
        expectedFingerprint: String, // DER HEX
        sessionId: String,
        fileId: String,
        transmissionId: String,
        inputStream: InputStream,
        fileSize: Long,
        fileName: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        Timber.d("session id from the client = $sessionId")
        val url = PeerApiRoutes.buildUploadUrl(ip, port, sessionId, fileId, transmissionId)

        val client = getClientWithFingerprintValidation(ip, expectedFingerprint)
        val requestBody = ProgressRequestBody(inputStream, fileSize, onProgress)

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .addHeader(CONTENT_TYPE, CONTENT_TYPE_OCTET)
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Timber.d("Upload successful for $fileId")
                    true
                } else {
                    Timber.e("Upload failed for $fileId with code ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while uploading $fileId")
            false
        }
    }

    suspend fun closeConnection(
        ip: String,
        port: String,
        expectedFingerprint: String,
        sessionId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val url = PeerApiRoutes.buildUrl(ip, port, PeerApiRoutes.CLOSE)

        val payload = Json.encodeToString(mapOf("sessionId" to sessionId))
        val requestBody = payload.toRequestBody()
        val client = getClientWithFingerprintValidation(ip, expectedFingerprint)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to close connection")
            false
        }
    }

    // ---------------- Internals ----------------

    private fun parseSessionIdFromResponse(body: String): RegisterPeerResult =
        try {
            val json = JSONObject(body)
            RegisterPeerResult.Success(json.getString("sessionId"))
        } catch (e: Exception) {
            Timber.e(e, "Malformed JSON response: %s", body)
            RegisterPeerResult.Failure(Exception("Malformed JSON: ${e.message}"))
        }

    private fun parseTransmissionId(body: String): PrepareUploadResult =
        try {
            val response = Json.decodeFromString<PeerPrepareUploadResponse>(body)
            PrepareUploadResult.Success(response.files)
        } catch (e: Exception) {
            Timber.e(e, "Invalid JSON response: %s", body)
            PrepareUploadResult.Failure(Exception("Malformed server response"))
        }

    private fun handleServerError(code: Int, body: String): PrepareUploadResult =
        when (code) {
            400 -> PrepareUploadResult.BadRequest
            403 -> PrepareUploadResult.Forbidden
            409 -> PrepareUploadResult.Conflict
            500 -> PrepareUploadResult.ServerError
            else -> PrepareUploadResult.Failure(Exception("Unhandled server error $code: $body"))
        }

    /**
     * Build an OkHttp client that:
     *  - Validates the server by DER-hash HEX (your getPublicKeyHash).
     *  - Routes sockets over Wi-Fi (LAN).
     *  - Relaxes hostname verification (safe because we hard-pin).
     */
    private fun getClientWithFingerprintValidation(
        ip: String,
        expectedFingerprintHex: String
    ): OkHttpClient {
        val expected = normalizeHex(expectedFingerprintHex)

        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String?) {
                val serverCert = chain.firstOrNull()
                    ?: throw CertificateException("Empty certificate chain")
                val actualHex = normalizeHex(CertificateUtils.getPublicKeyHash(serverCert))
                if (actualHex != expected) {
                    throw CertificateException(
                        "Certificate DER hash mismatch.\nExpected: $expected\nGot:      $actualHex"
                    )
                }
            }
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }

        val builder = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true } // IP literal + pinning → OK
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)

        // Route over Wi-Fi (same network path as FingerprintFetcher)
        findWifiNetwork(appContext)?.let { builder.socketFactory(it.socketFactory) }

        return builder.build()
    }

    private fun findWifiNetwork(context: Context): Network? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.allNetworks.firstOrNull { n ->
            cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    private fun normalizeHex(hexLike: String): String =
        hexLike.trim().replace(":", "").replace("\\s".toRegex(), "").lowercase()
}
