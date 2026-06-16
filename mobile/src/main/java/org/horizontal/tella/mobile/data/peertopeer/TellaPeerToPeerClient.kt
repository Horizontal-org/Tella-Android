package org.horizontal.tella.mobile.data.peertopeer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.TlsVersion
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.PeerMtlsSsl
import org.horizontal.tella.mobile.data.peertopeer.PeerKeyProvider
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants.CONTENT_TYPE
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants.CONTENT_TYPE_JSON
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants.CONTENT_TYPE_OCTET
import org.horizontal.tella.mobile.data.peertopeer.network.ProgressRequestBody
import org.horizontal.tella.mobile.data.peertopeer.remote.PeerApiRoutes
import org.horizontal.tella.mobile.data.peertopeer.remote.PeerUploadOutcome
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.data.peertopeer.remote.PeerPingResult
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadResult
import org.horizontal.tella.mobile.data.peertopeer.remote.RegisterPeerResult
import org.horizontal.tella.mobile.domain.peertopeer.P2PFile
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.json.JSONObject
import timber.log.Timber
import java.io.InputStream
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class TellaPeerToPeerClient @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private const val REGISTER_READ_TIMEOUT_SEC = 120L
    }

    suspend fun registerPeerDevice(
        ip: String,
        port: String,
        expectedFingerprint: String,
        pin: String,
        nonce: String,
    ): RegisterPeerResult = withContext(Dispatchers.IO) {
        val url = PeerApiRoutes.buildUrl(ip, port, PeerApiRoutes.REGISTER)
        Timber.d("Connecting to: $url")

        val payload = PeerRegisterPayload(
            pin = pin.trim(),
            nonce = nonce,
        )

        val jsonPayload = Json.encodeToString(payload)
        val requestBody = jsonPayload.toRequestBody(CONTENT_TYPE_JSON.toMediaType())
        val client = getMtlsClient(ip, expectedFingerprint, forRegistration = true)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
            // Encourage short-lived TLS sessions to avoid half-closed sockets across platforms.
            .addHeader("Connection", "close")
            .build()

        Timber.d("registerPeerDevice payload=%s", jsonPayload)

        return@withContext try {
            client.newCall(request).execute().use { response ->
                response.handshake?.let { hs ->
                    Timber.d(
                        "registerPeerDevice TLS localCerts=%d peerCerts=%d",
                        hs.localCertificates.size,
                        hs.peerCertificates.size,
                    )
                }
                val body = response.body.string()
                Timber.d("registerPeerDevice code=%d body=%s", response.code, body.take(300))

                if (response.isSuccessful) {
                    when (val parsed = parseSessionIdFromResponse(body)) {
                        is RegisterPeerResult.Success -> parsed
                        is RegisterPeerResult.Failure -> parsed
                        else -> RegisterPeerResult.Failure(Exception("Unexpected success response shape"))
                    }
                } else {
                    when (response.code) {
                        400 -> parseRegisterBadRequest(body)
                        401 -> RegisterPeerResult.InvalidPin
                        403 -> RegisterPeerResult.RejectedByReceiver
                        409 -> RegisterPeerResult.Conflict
                        429 -> RegisterPeerResult.TooManyRequests
                        500 -> RegisterPeerResult.ServerError
                        else -> RegisterPeerResult.Failure(Exception("Unhandled error ${response.code}: $body"))
                    }
                }
            }
        } catch (e: Exception) {
            RegisterPeerResult.Failure(e)
        }
    }

    suspend fun prepareUpload(
        ip: String,
        port: String,
        expectedFingerprint: String, // leaf cert DER SHA-256 hex (see CertificateUtils.getLeafCertificateDerSha256Hex)
        title: String,
        /** Plaintext file metadata: [P2PFile.sha256] and [P2PFile.size] must match the PUT body bytes. */
        files: List<P2PFile>,
        sessionId: String
    ): PrepareUploadResult = withContext(Dispatchers.IO) {
        val url = PeerApiRoutes.buildUrl(ip, port, PeerApiRoutes.PREPARE_UPLOAD)

        val requestPayload = PrepareUploadRequest(
            title = title,
            sessionId = sessionId,
            nonce = UUID.randomUUID().toString(),
            files = files,
        )
        val jsonPayload = Json.encodeToString(requestPayload)
        val requestBody = jsonPayload.toRequestBody(CONTENT_TYPE_JSON.toMediaType())
        val client = getMtlsClient(ip, expectedFingerprint)

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                .addHeader("Connection", "close")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                Timber.d("prepareUpload: code=%d body=%s", response.code, responseBody.take(600))
                if (response.isSuccessful) {
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
        expectedFingerprint: String, // leaf cert DER SHA-256 hex (see CertificateUtils.getLeafCertificateDerSha256Hex)
        sessionId: String,
        fileId: String,
        transmissionId: String,
        inputStream: InputStream,
        fileSize: Long,
        fileName: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
    ): PeerUploadOutcome = withContext(Dispatchers.IO) {
        Timber.d("session id from the client = %s", sessionId)
        val uploadNonce = UUID.randomUUID().toString()
        val url = PeerApiRoutes.buildUploadUrl(
            ip, port, sessionId, fileId, transmissionId, uploadNonce
        )

        val client = getMtlsClient(ip, expectedFingerprint)
        val requestBody = ProgressRequestBody(inputStream, fileSize, onProgress)

        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .addHeader(CONTENT_TYPE, CONTENT_TYPE_OCTET)
            .addHeader("Connection", "close")
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val outcome = when (code) {
                    429 -> {
                        PeerUploadOutcome.TooManyRequests
                    }

                    409 -> {
                        PeerUploadOutcome.Failed
                    }

                    413 -> {
                        PeerUploadOutcome.PayloadTooLarge
                    }

                    406 -> {
                        PeerUploadOutcome.Failed
                    }

                    else -> if (response.isSuccessful) {
                        PeerUploadOutcome.Success
                    } else {
                        PeerUploadOutcome.Failed
                    }
                }
                outcome
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while uploading %s", fileId)
            PeerUploadOutcome.Failed
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
        val requestBody = payload.toRequestBody(CONTENT_TYPE_JSON.toMediaType())
        val client = getMtlsClient(ip, expectedFingerprint)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
            .addHeader("Connection", "close")
            .build()

        return@withContext try {
            client.newCall(request)

                .execute().use { response ->
                    if (response.code == 429) {
                        Timber.w("closeConnection: rate limited (429)")
                    }
                    Timber.d("closeConnection: code=%d", response.code)
                    response.isSuccessful
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to close connection")
            false
        }
    }

    // ---------------- Internals ----------------

    private fun parseRegisterBadRequest(body: String): RegisterPeerResult =
        when {
            body.contains("Client certificate required", ignoreCase = true) ->
                RegisterPeerResult.ClientCertificateRequired
            else -> RegisterPeerResult.InvalidFormat
        }

    private fun parseSessionIdFromResponse(body: String): RegisterPeerResult {
        return try {
            val json = JSONObject(body)

            // Optional: some servers send { success: true/false }
            val successFlag = json.optBoolean("success", true)

            // Required: non-empty sessionId
            val sessionId = json.optString("sessionId", "").trim()

            when {
                !successFlag -> {
                    val msg =
                        json.optString("message", json.optString("error", "Registration rejected"))
                    RegisterPeerResult.Failure(Exception(msg))
                }

                sessionId.isEmpty() -> {
                    RegisterPeerResult.Failure(Exception("Missing or empty sessionId"))
                }

                else -> RegisterPeerResult.Success(sessionId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Malformed JSON response: %s", body)
            RegisterPeerResult.Failure(Exception("Malformed JSON: ${e.message}"))
        }
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
            413 -> PrepareUploadResult.PayloadTooLarge
            429 -> PrepareUploadResult.TooManyRequests
            500 -> PrepareUploadResult.ServerError
            else -> PrepareUploadResult.Failure(Exception("Unhandled server error $code: $body"))
        }

    /**
     * OkHttp client that:
     *  - Pins the server by leaf certificate DER SHA-256 hex (CertificateUtils.getLeafCertificateDerSha256Hex).
     *  - Optionally binds sockets to a Wi-Fi Network if one is active/validated.
     *  - Keeps default hostname verification (IP SAN must match when using IP literals).
     *  - TLS: OkHttp is configured with [TlsVersion.TLS_1_3] first, then [TlsVersion.TLS_1_2] (prefers 1.3 on both peers when
     *    the stack supports it; e.g. TLS 1.3 is available from Android 10 / API 29 onward). A strict 1.3-only client was
     *    considered for parity with iOS defaults, but would block Nearby Sharing on minSdk 21 devices where 1.3 is
     *    unavailable—so we keep 1.2+1.3, matching the product call on the cross-platform thread (Feb 18 discussion).
     */
    private fun senderIdentity() = PeerKeyProvider.ensureSenderIdentity()

    private fun getMtlsClient(
        ip: String,
        expectedFingerprintHex: String,
        requirePinnedReceiver: Boolean = true,
        serverCertCaptor: PeerServerCertCapturingTrustManager? = null,
        forRegistration: Boolean = false,
    ): OkHttpClient {
        val (senderKeyPair, senderCert) = senderIdentity()
        val pinned = if (requirePinnedReceiver) normalizeHex(expectedFingerprintHex) else null
        val baseTrustManager: X509TrustManager = if (pinned.isNullOrEmpty()) {
            CertificateUtils.getFingerprintCollectionTrustManager()
        } else {
            CertificateUtils.getLeafCertPinnedTrustManager(pinned)
        }
        val trustManager: X509TrustManager = serverCertCaptor ?: baseTrustManager
        val sslContext = PeerMtlsSsl.createSenderSslContext(
            senderKeyPair = senderKeyPair,
            senderCertificate = senderCert,
            pinnedReceiverHash = pinned,
            trustManagerOverride = trustManager,
        )

        val tlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .allEnabledCipherSuites()
            .build()

        val readTimeoutSec = if (forRegistration) REGISTER_READ_TIMEOUT_SEC else 20L
        val builder = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectionSpecs(listOf(tlsSpec))
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(readTimeoutSec, TimeUnit.SECONDS)

        pickWifiNetworkForP2P(appContext)?.let { network ->
            builder.socketFactory(network.socketFactory)
        }

        return builder.build()
    }

    /** Any Wi-Fi network — local-only / hotspot links often lack VALIDATED or INTERNET. */
    @Suppress("DEPRECATION")
    private fun pickWifiNetworkForP2P(context: Context): Network? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.allNetworks.firstOrNull { n ->
                cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }?.let { return it }
        }
        return cm.allNetworks.firstOrNull { n ->
            val info = cm.getNetworkInfo(n)
            info?.isConnected == true && info.type == ConnectivityManager.TYPE_WIFI
        }
    }

    private fun normalizeHex(hexLike: String): String =
        hexLike.trim().replace(":", "").replace("\\s".toRegex(), "").lowercase()


  /**
   * Protocol v2 initial ping with sender client certificate attached.
   * Returns the receiver leaf cert hash (from the TLS handshake) plus the `senderShowHash` flag
   * parsed from the response body (protocol §3.1), or null on failure.
   *
   * Per the protocol security note, callers must only act on [PeerPingResult.senderShowHash] AFTER the
   * receiver hash has been verified — the ping channel isn't authenticated until then.
   */
    suspend fun pingAndFetchReceiverHash(ip: String, port: String): PeerPingResult? =
        withContext(Dispatchers.IO) {
            val url = PeerApiRoutes.buildUrl(ip, port, PeerApiRoutes.PING)
            val serverCertCaptor = PeerServerCertCapturingTrustManager(
                CertificateUtils.getFingerprintCollectionTrustManager()
            )
            val client = getMtlsClient(
                ip,
                expectedFingerprintHex = "",
                requirePinnedReceiver = false,
                serverCertCaptor = serverCertCaptor,
            )
            val req = Request.Builder()
                .url(url)
                .post(ByteArray(0).toRequestBody())
                .addHeader("Connection", "close")
                .build()

            runCatching {
                client.newCall(req).execute().use { resp ->
                    if (resp.code == 429) {
                        Timber.w("pingAndFetchReceiverHash: rate limited (429)")
                    }
                    if (!resp.isSuccessful) {
                        Timber.w("pingAndFetchReceiverHash $url -> HTTP %d", resp.code)
                        return@withContext null
                    }
                    val body = resp.body.string()
                    // Defaults to false when the field/body is absent (older or non-conforming peer),
                    val senderShowHash = runCatching {
                        JSONObject(body).optBoolean("senderShowHash", false)
                    }.getOrDefault(false)
                    // SSLSession does not always expose peer certs (see sender log peerCerts=0),
                    // so fall back to the cert recorded by our trust manager during the handshake.
                    val handshakeCert = (resp.handshake?.peerCertificates?.firstOrNull()
                        as? java.security.cert.X509Certificate)
                        ?: serverCertCaptor.lastServerLeaf
                    if (handshakeCert == null) {
                        Timber.w("pingAndFetchReceiverHash: no server cert from handshake or captor")
                        return@withContext null
                    }
                    val receiverHash = CertificateUtils.getLeafCertificateDerSha256Hex(handshakeCert)
                    PeerPingResult(receiverHash = receiverHash, senderShowHash = senderShowHash)
                }
            }.getOrElse {
                Timber.w(it, "Ping failed for $url")
                null
            }
        }

    suspend fun pingBeforeRegister(ip: String, port: String): Boolean =
        pingAndFetchReceiverHash(ip, port) != null


}
