package org.horizontal.tella.mobile.data.peertopeer

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.request.receiveStream
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerEventManager
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.horizontal.tella.mobile.domain.peertopeer.PeerResponse
import org.horizontal.tella.mobile.domain.peertopeer.TellaServer
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.UploadProgressState
import timber.log.Timber
import java.io.File
import java.security.KeyPair
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.UUID

const val port = 53317

class TellaPeerToPeerServer(
    private val ip: String,
    private val serverPort: Int = port,
    private val pin: String,
    private val keyPair: KeyPair,
    private val certificate: X509Certificate,
    private val keyStoreConfig: KeyStoreConfig,
    private val peerToPeerManager: PeerToPeerManager,
    private val p2PSharedState: P2PSharedState
) : TellaServer {
    private var serverSession: PeerResponse? = null
    private var engine: ApplicationEngine? = null

    override val certificatePem: String
        get() = CertificateUtils.certificateToPem(certificate)

    override fun start() {
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry(
                keyStoreConfig.alias,
                keyPair.private,
                keyStoreConfig.password,
                arrayOf(certificate)
            )
        }

        engine = embeddedServer(Netty, environment = applicationEngineEnvironment {
            sslConnector(
                keyStore = keyStore,
                keyAlias = keyStoreConfig.alias,
                keyStorePassword = { keyStoreConfig.password },
                privateKeyPassword = { keyStoreConfig.password }
            ) {
                this.host = ip
                this.port = serverPort
            }

            module {
                install(ContentNegotiation) {
                    json()
                }
                routing {
                    // Root route to confirm the server is running
                    get("/") {
                        call.respondText("The server is running securely over HTTPS.")
                    }

                    post(PeerApiRoutes.PING) {
                        CoroutineScope(Dispatchers.IO).launch {
                            peerToPeerManager.notifyClientConnected(p2PSharedState.hash)
                        }
                        call.respondText("ping", status = HttpStatusCode.OK)
                    }

                    post(PeerApiRoutes.REGISTER) {
                        val request = try {
                            call.receive<PeerRegisterPayload>()
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid request format")
                            return@post
                        }

                        if (!isValidPin(request.pin) || pin != request.pin) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid PIN")
                            return@post
                        }

                        if (serverSession != null) {
                            call.respond(HttpStatusCode.Conflict, "Active session already exists")
                            return@post
                        }

                        val sessionId = UUID.randomUUID().toString()
                        val session = PeerResponse(sessionId)
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
                                HttpStatusCode.Forbidden,
                                "Receiver rejected the registration"
                            )
                            return@post
                        }

                        launch {
                            PeerEventManager.emitRegistrationSuccess()
                        }

                        call.respond(HttpStatusCode.OK, session)
                    }

                    post(PeerApiRoutes.PREPARE_UPLOAD) {
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

                        if (request.sessionId != serverSession?.sessionId) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid session ID")
                            return@post
                        }

                        val accepted = PeerEventManager.emitPrepareUploadRequest(request)
                        if (accepted) {

                            val session =
                                P2PSession(title = request.title, sessionId = request.sessionId)

                            val responseFiles = request.files.map { file ->
                                val transmissionId = UUID.randomUUID().toString()
                                val receivingFile = ProgressFile(
                                    file = file,
                                    transmissionId = transmissionId
                                )
                                session.files[transmissionId] = receivingFile

                                FileInfo(
                                    id = file.id,
                                    transmissionId = transmissionId
                                )
                            }

                            p2PSharedState.session = session

                            val responsePayload = PeerPrepareUploadResponse(files = responseFiles)
                            call.respond(HttpStatusCode.OK, responsePayload)
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "Transfer rejected by receiver")
                        }
                    }

                    put(PeerApiRoutes.UPLOAD) {
                        val sessionId = call.parameters["sessionId"]
                        val fileId = call.parameters["fileId"]
                        val transmissionId = call.parameters["transmissionId"]

                        Timber.d("session id from the server +$sessionId")

                        if (sessionId == null || fileId == null || transmissionId == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing path parameters")
                            return@put
                        }

                        val session = p2PSharedState.session
                        if (sessionId != session?.sessionId) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid session ID")
                            return@put
                        }

                        val progressFile = session.files[transmissionId]
                        if (progressFile == null || progressFile.file.id != fileId) {
                            call.respond(HttpStatusCode.NotFound, "File not found in session")
                            return@put
                        }

                        val tmpFile = File.createTempFile(fileId, ".tmp")
                        val output = tmpFile.outputStream().buffered()

                        try {
                            val input = call.receiveStream().buffered()
                            val totalSize = session.files.values.sumOf { it.file.size }
                            var bytesRead = 0L
                            val buffer = ByteArray(8192)

                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                bytesRead += read
                                progressFile.bytesTransferred = bytesRead.toInt()

                                // Optional: update session progress percent
                                val totalTransferred = session.files.values.sumOf { it.bytesTransferred }
                                val percent = if (totalSize > 0) ((totalTransferred * 100) / totalSize).toInt() else 0

                                val state = UploadProgressState(
                                    title = session.title ?: "",
                                    percent = percent,
                                    sessionStatus = session.status,
                                    files = session.files.values.toList()
                                )
                                PeerEventManager.onUploadProgressState(state)
                            }

                            progressFile.status = P2PFileStatus.FINISHED
                            progressFile.path = tmpFile.path

                            val finalState = UploadProgressState(
                                title = session.title ?: "",
                                percent = 100,
                                sessionStatus = SessionStatus.FINISHED,
                                files = session.files.values.toList()
                            )

                            PeerEventManager.onUploadProgressState(finalState)

                            call.respond(HttpStatusCode.OK, "Upload complete")
                        } catch (e: Exception) {
                            progressFile.status = P2PFileStatus.FAILED

                            val failedState = UploadProgressState(
                                title = session.title ?: "",
                                percent = 0,
                                sessionStatus = session.status,
                                files = session.files.values.toList()
                            )
                            PeerEventManager.onUploadProgressState(failedState)

                            call.respond(HttpStatusCode.InternalServerError, "Upload failed: ${e.message}")
                        } finally {
                            output.close()
                        }
                    }

                    post(PeerApiRoutes.CLOSE) {
                        val payload = try {
                            call.receive<Map<String, String>>()
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "Missing or invalid JSON payload")
                            return@post
                        }

                        val sessionId = payload["sessionId"]

                        if (sessionId.isNullOrBlank()) {
                            call.respond(HttpStatusCode.BadRequest, "Missing session ID")
                            return@post
                        }

                        val currentSession = serverSession
                        if (currentSession == null) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid session ID")
                            return@post
                        }

                        if (currentSession.sessionId != sessionId) {
                            call.respond(HttpStatusCode.Unauthorized, "Invalid session ID")
                            return@post
                        }

                        if (p2PSharedState.session?.status == SessionStatus.CLOSED) {
                            call.respond(HttpStatusCode.Forbidden, "Session already closed")
                            return@post
                        }

                        try {
                            // ✅ Mark session as closed
                            p2PSharedState.session?.status = SessionStatus.CLOSED
                            serverSession = null

                            call.respond(HttpStatusCode.OK, mapOf("success" to true))
                        } catch (e: Exception) {
                            Timber.e(e, "Error while closing session")
                            call.respond(HttpStatusCode.InternalServerError, "Server error")
                        }
                    }

                }

            }
        }).start(wait = false)
    }

    override fun stop() {
        engine?.stop(1000, 5000)
    }

    private fun isValidPin(pin: String): Boolean {
        return pin.length == 6
    }
}
