package org.horizontal.tella.mobile.data.peertopeer

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSession
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.model.ProgressFile
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus
import org.horizontal.tella.mobile.data.peertopeer.remote.PeerApiRoutes
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.domain.peertopeer.FileInfo
import org.horizontal.tella.mobile.domain.peertopeer.NearbySharingTransferConfig
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerEventManager
import org.horizontal.tella.mobile.data.peertopeer.P2PNetworkAddressPolicy
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants.NEARBY_SHARING_TLS_PORT
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.horizontal.tella.mobile.domain.peertopeer.PeerResponse
import org.horizontal.tella.mobile.domain.peertopeer.TellaServer
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.UploadProgressState
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.security.KeyPair
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

const val port = NEARBY_SHARING_TLS_PORT

private const val MAX_REGISTER_PIN_ATTEMPTS = 3

class TellaPeerToPeerServer(
    /**
     * IPv4 shown in QR and used for TLS cert SAN; the Netty server binds to [P2PNetworkAddressPolicy.INADDR_ANY_IPV4].
     */
    private val advertisedHost: String,
    private val serverPort: Int = port,
    private val pin: String,
    private val keyPair: KeyPair,
    private val certificate: X509Certificate,
    private val keyStoreConfig: KeyStoreConfig,
    private val peerToPeerManager: PeerToPeerManager,
    private val p2PSharedState: P2PSharedState,
    private val receiveDir: File,
    private val rateLimitConfig: PeerServerRateLimitConfig = PeerServerRateLimitConfig.DEFAULT,
) : TellaServer {

    private var serverSession: PeerResponse? = null
    private var embedded: EmbeddedServer<*, *>? = null
    private val transferNonceManager = TransferNonceManager()
    private val registerPinFailuresByNonce = ConcurrentHashMap<String, Int>()
    private val rateLimiter = PeerTimedRateLimiter(rateLimitConfig)

    override val certificatePem: String
        get() = CertificateUtils.certificateToPem(certificate)

    private suspend fun emitReceiveProgress(session: P2PSession) {
        val totalTransferred = session.files.values.sumOf { it.bytesTransferred.toLong() }
        val totalSize = session.files.values.sumOf { it.file.size }
        val percent =
            if (totalSize > 0) ((totalTransferred * 100) / totalSize).toInt() else 0
        PeerEventManager.onUploadProgressState(
            UploadProgressState(
                title = session.title.orEmpty(),
                percent = percent,
                sessionStatus = session.status,
                files = session.files.values.toList()
            )
        )
    }

    override fun start() {
        Timber.e(
            "SERVER STARTED bind=%s port=%d advertisedToPeer=%s",
            P2PNetworkAddressPolicy.INADDR_ANY_IPV4,
            serverPort,
            advertisedHost,
        )
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry(
                keyStoreConfig.alias, keyPair.private, keyStoreConfig.password, arrayOf(certificate)
            )
        }

        embedded = embeddedServer(
            Netty,
            environment = applicationEnvironment { },
            configure = {
                enableHttp2 = false
                enableH2c = false
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = keyStoreConfig.alias,
                    keyStorePassword = { keyStoreConfig.password },
                    privateKeyPassword = { keyStoreConfig.password },
                ) {
                    host = P2PNetworkAddressPolicy.INADDR_ANY_IPV4
                    port = serverPort
                    enabledProtocols = listOf("TLSv1.3", "TLSv1.2")
                }
            },
        ) {
            install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }

            intercept(ApplicationCallPipeline.Call) {
                val clientIp = call.peerClientIpForRateLimit()
                val routePath = call.peerRoutePathForRateLimit()
                if (rateLimiter.isLimited(clientIp, routePath)) {
                    rateLimitConfig.retryAfterSecondsWhenLimited?.let { secs ->
                        call.response.headers.append(HttpHeaders.RetryAfter, secs.toString())
                    }
                    call.respondText(
                        """{"error":"Too many requests"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.TooManyRequests,
                    )
                    finish()
                    return@intercept
                }
            }

            routing {
                // Sanity probe
                get("/") {
                    call.respondText("The server is running securely over HTTPS.")
                }

                // Client presence hint (optional)
                post(PeerApiRoutes.PING) {
                    val clientIp = call.peerClientIpForRateLimit()
                    CoroutineScope(Dispatchers.IO).launch {
                        peerToPeerManager.notifyClientConnected(p2PSharedState.hash)
                    }
                    call.respondText("ping", status = HttpStatusCode.OK)
                }

                // 1) Register a session
                post(PeerApiRoutes.REGISTER) {
                    try {
                        val request = try {
                            call.receive<PeerRegisterPayload>()
                        } catch (_: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid request format")
                            return@post
                        }

                        // Active session, then nonce / PIN limits
                        if (serverSession != null) {
                            call.respond(HttpStatusCode.Conflict, "Active session already exists")
                            return@post
                        }

                        val regNonce = request.nonce
                        if (regNonce == null || regNonce.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid request format")
                            return@post
                        }

                        if ((registerPinFailuresByNonce[regNonce]
                                ?: 0) >= MAX_REGISTER_PIN_ATTEMPTS
                        ) {
                            call.respond(HttpStatusCode.TooManyRequests, "Too many requests")
                            return@post
                        }

                        if (!isValidPin(request.pin) || pin != request.pin) {
                            val count = (registerPinFailuresByNonce[regNonce] ?: 0) + 1
                            registerPinFailuresByNonce[regNonce] = count
                            if (count >= MAX_REGISTER_PIN_ATTEMPTS) {
                                call.respond(HttpStatusCode.TooManyRequests, "Too many requests")
                            } else {
                                call.respond(HttpStatusCode.Unauthorized, "Invalid PIN")
                            }
                            return@post
                        }

                        registerPinFailuresByNonce.remove(regNonce)

                        val sessionId = UUID.randomUUID().toString()
                        val session = PeerResponse(sessionId)
                        if (p2PSharedState.session == null) {
                            p2PSharedState.session = P2PSession()
                        }
                        p2PSharedState.session?.sessionId = sessionId
                        serverSession = session

                        val accepted = try {
                            PeerEventManager.emitIncomingRegistrationRequest(sessionId, request)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, "Internal error")
                            return@post
                        }

                        if (!accepted) {
                            call.respond(
                                HttpStatusCode.Forbidden, "Receiver rejected the registration"
                            )
                            return@post
                        }

                        launch { PeerEventManager.emitRegistrationSuccess() }
                        call.respond(HttpStatusCode.OK, session)
                    } catch (e: Exception) {
                        try {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Internal server error",
                            )
                        } catch (_: Exception) {
                            // Response may already be committed
                        }
                    }
                }

                // 2) Prepare upload → create receiving session, STATUS = SENDING
                post(PeerApiRoutes.PREPARE_UPLOAD) {
                    try {
                        val request = try {
                            call.receive<PrepareUploadRequest>()
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid body: ${e.message}")
                            return@post
                        }

                        if (request.title.isBlank() || request.sessionId.isBlank() || request.files.isEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, "Missing required fields")
                            return@post
                        }

                        if (request.files.any { it.sha256.isNullOrBlank() }) {
                            call.respond(HttpStatusCode.BadRequest, "Missing file hash")
                            return@post
                        }

                        if (request.files.size > NearbySharingTransferConfig.Standard.maxFileCount) {
                            call.respond(HttpStatusCode.PayloadTooLarge, "Content too large")
                            return@post
                        }
                        if (request.files.any { it.size > NearbySharingTransferConfig.Standard.maxFileSizeBytes }) {
                            call.respond(HttpStatusCode.PayloadTooLarge, "Content too large")
                            return@post
                        }

                        if (request.sessionId != serverSession?.sessionId) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid session ID")
                            return@post
                        }

                        when (transferNonceManager.tryAdd(request.nonce)) {
                            TransferNonceManager.AddResult.Empty,
                            TransferNonceManager.AddResult.Reused -> {
                                call.respond(HttpStatusCode.Conflict, "Invalid nonce")
                                return@post
                            }

                            TransferNonceManager.AddResult.Success -> { /* continue */
                            }
                        }

                        val accepted = PeerEventManager.emitPrepareUploadRequest(request)
                        if (!accepted) {
                            call.respond(HttpStatusCode.Forbidden, "Transfer rejected by receiver")
                            return@post
                        }

                        val session = P2PSession(
                            title = request.title, sessionId = request.sessionId
                        ).also { it.status = SessionStatus.SENDING }

                        val responseFiles = request.files.map { file ->
                            val transmissionId = UUID.randomUUID().toString()
                            val receivingFile =
                                ProgressFile(file = file, transmissionId = transmissionId)
                            session.files[transmissionId] = receivingFile
                            FileInfo(id = file.id, transmissionId = transmissionId)
                        }

                        p2PSharedState.session = session
                        call.respond(
                            HttpStatusCode.OK, PeerPrepareUploadResponse(files = responseFiles)
                        )
                    } catch (e: Exception) {
                        try {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Internal server error",
                            )
                        } catch (_: Exception) {
                        }
                    }
                }

                // 3) Upload each file (transport-only; recipient will SAVE later)
                put(PeerApiRoutes.UPLOAD) {
                    try {
                        val sessionId = call.parameters["sessionId"]
                        val fileId = call.parameters["fileId"]
                        val transmissionId = call.parameters["transmissionId"]
                        val uploadNonce = call.parameters["nonce"]


                        // beginUploadFromRequest: required ids first (400), then nonce (409), then session
                        if (sessionId == null || fileId == null || transmissionId == null ||
                            sessionId.isEmpty() || fileId.isEmpty() || transmissionId.isEmpty()
                        ) {
                            call.respond(HttpStatusCode.BadRequest, "Missing path parameters")
                            return@put
                        }

                        when (transferNonceManager.tryAdd(uploadNonce)) {
                            TransferNonceManager.AddResult.Empty,
                            TransferNonceManager.AddResult.Reused -> {
                                call.respond(HttpStatusCode.Conflict, "Invalid nonce")
                                return@put
                            }

                            TransferNonceManager.AddResult.Success -> { /* continue */
                            }
                        }

                        val session = p2PSharedState.session
                        if (sessionId != session?.sessionId) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid session ID")
                            return@put
                        }

                        val progressFile = session.files[transmissionId]

                        if (transmissionId != progressFile?.transmissionId) {
                            call.respond(HttpStatusCode.Forbidden, "Invalid transmission ID")
                            return@put
                        }

                        if (progressFile.file.id != fileId) {
                            call.respond(HttpStatusCode.NotFound, "File not found in session")
                            return@put
                        }

                        if (progressFile.status == P2PFileStatus.FINISHED) {
                            call.respond(HttpStatusCode.Conflict, "Transfer already completed")
                            return@put
                        }

                        val declaredSize = progressFile.file.size
                        if (declaredSize > NearbySharingTransferConfig.Standard.maxFileSizeBytes ||
                            declaredSize < 0
                        ) {
                            progressFile.status = P2PFileStatus.FAILED
                            emitReceiveProgress(session)
                            call.respond(HttpStatusCode.PayloadTooLarge, "Content too large")
                            return@put
                        }

                        val tmpFile = File.createTempFile(
                            sanitizeFileIdForTempPrefix(fileId),
                            ".tmp",
                            receiveDir
                        )
                        var completed = false
                        var input: BufferedInputStream? = null
                        var output: BufferedOutputStream? = null
                        try {
                            output = tmpFile.outputStream().buffered()
                            input = call.receiveStream().buffered()
                            var bytesRead = 0L
                            val buffer = ByteArray(8192)

                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                if (bytesRead + read > declaredSize) {
                                    progressFile.status = P2PFileStatus.FAILED
                                    emitReceiveProgress(session)
                                    call.respond(
                                        HttpStatusCode.PayloadTooLarge,
                                        "Content too large"
                                    )
                                    return@put
                                }
                                output.write(buffer, 0, read)
                                bytesRead += read
                                progressFile.bytesTransferred = bytesRead.toInt()
                                emitReceiveProgress(session)
                            }

                            // Ensure all bytes are on disk before hashing (BufferedOutputStream).
                            output.flush()

                            val expected = progressFile.file.sha256?.trim().orEmpty()
                            if (expected.isEmpty()) {
                                progressFile.status = P2PFileStatus.FAILED
                                emitReceiveProgress(session)
                                call.respond(HttpStatusCode.NotAcceptable, "File hash mismatch")
                                return@put
                            }

                            val actual = PeerFileHash.sha256Hex(tmpFile)
                            if (!actual.equals(expected, ignoreCase = true)) {
                                progressFile.status = P2PFileStatus.FAILED
                                emitReceiveProgress(session)
                                call.respond(HttpStatusCode.NotAcceptable, "File hash mismatch")
                                return@put
                            }

                            progressFile.status = P2PFileStatus.FINISHED
                            progressFile.path = tmpFile.path

                            emitReceiveProgress(session)

                            completed = true
                            call.respond(HttpStatusCode.OK, "Upload complete")
                        } catch (e: Exception) {
                            progressFile.status = P2PFileStatus.FAILED
                            emitReceiveProgress(session)
                            call.respond(
                                HttpStatusCode.InternalServerError, "Upload failed: ${e.message}"
                            )
                        } finally {
                            try {
                                input?.close()
                            } catch (_: Exception) {
                                Timber.w("P2P upload: input close failed")
                            }
                            try {
                                output?.flush()
                            } catch (_: Exception) {
                                // ignore
                            }
                            try {
                                output?.close()
                            } catch (_: Exception) {
                                Timber.w("P2P upload: output close failed")
                            }
                            if (!completed) {
                                if (!tmpFile.delete()) {
                                    // Rare: FS delay; avoid noisy warning for benign leftovers in cache.
                                    Timber.d(
                                        "P2P upload: temp delete deferred or failed %s",
                                        tmpFile.path
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "P2P /upload unhandled exception")
                        try {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Internal server error",
                            )
                        } catch (_: Exception) {
                        }
                    }
                }

                // 4) Close session (transport finished/cancelled by sender)
                post(PeerApiRoutes.CLOSE) {
                    val payload = try {
                        call.receive<Map<String, String>>()
                    } catch (_: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest, "Missing or invalid JSON payload"
                        )
                        return@post
                    }

                    val sessionId = payload["sessionId"]
                    if (sessionId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid request format")
                        return@post
                    }

                    val current = serverSession
                    if (current == null || current.sessionId != sessionId) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid session ID")
                        return@post
                    }

                    if (p2PSharedState.session?.status == SessionStatus.CLOSED) {
                        call.respond(HttpStatusCode.Forbidden, "Session already closed")
                        return@post
                    }

                    try {
                        p2PSharedState.session?.status = SessionStatus.CLOSED
                        serverSession = null
                        transferNonceManager.clear()
                        launch { PeerEventManager.emitCloseConnection() }
                        call.respond(HttpStatusCode.OK, mapOf("success" to true))
                    } catch (e: Exception) {
                        Timber.e(e, "Error while closing session")
                        call.respond(HttpStatusCode.InternalServerError, "Server error")
                    }
                }

            }
        }.start(wait = false)

        CoroutineScope(Dispatchers.IO).launch {
            delay(2_000)
            Timber.e(
                "SERVER READY bind=%s port=%d advertisedToPeer=%s",
                P2PNetworkAddressPolicy.INADDR_ANY_IPV4,
                serverPort,
                advertisedHost,
            )
        }
    }

    override fun stop() {
        transferNonceManager.clear()
        registerPinFailuresByNonce.clear()
        try {
            embedded?.stop(1000, 5000)
        } finally {
            embedded = null
        }
    }

    private fun isValidPin(pin: String) = pin.length == 6

    /**
     * [File.createTempFile] requires prefix length ≥ 3; strip path-like characters for safety.
     */
    private fun sanitizeFileIdForTempPrefix(fileId: String): String {
        val sb = StringBuilder(fileId.length.coerceAtMost(128))
        for (c in fileId) {
            when {
                c.isLetterOrDigit() -> sb.append(c)
                c == '-' || c == '_' -> sb.append(c)
                else -> sb.append('_')
            }
        }
        var base = sb.toString().trim('_')
        if (base.length < 3) {
            base = "p2p_$base".trim('_')
        }
        if (base.length < 3) {
            base = "p2precv"
        }
        return base.take(120)
    }
}
