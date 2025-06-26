package org.horizontal.tella.mobile.data.peertopeer

import android.util.Log
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
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.data.peertopeer.managers.PeerToPeerManager
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.horizontal.tella.mobile.domain.peertopeer.PeerResponse
import org.horizontal.tella.mobile.domain.peertopeer.TellaServer
import org.horizontal.tella.mobile.views.fragment.peertopeer.PeerEventManager
import java.security.KeyPair
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.UUID

const val port = 53317

class TellaPeerToPeerServer(
    private val ip: String,
    private val serverPort: Int = port,
    private val keyPair: KeyPair,
    private val certificate: X509Certificate,
    private val keyStoreConfig: KeyStoreConfig,
    private val peerToPeerManager: PeerToPeerManager
) : TellaServer {

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
                        Log.i("Test", "Server started")
                        call.respondText("The server is running securely over HTTPS.")
                    }

                    post("/api/v1/ping") {
                        val hash = CertificateUtils.getPublicKeyHash(certificate)
                        CoroutineScope(Dispatchers.IO).launch {
                            peerToPeerManager.notifyClientConnected(hash)
                        }
                        call.respondText("pong", status = HttpStatusCode.OK)
                    }

                    post("/api/v1/register") {
                        val request = try {
                            call.receive<PeerRegisterPayload>()
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                            return@post
                        }

                        val registrationId = UUID.randomUUID().toString()

                        val accepted = PeerEventManager.emitIncomingRegistrationRequest(registrationId, request)

                        if (!accepted) {
                            call.respond(HttpStatusCode.Forbidden, "Receiver rejected the registration")
                            return@post
                        }

                        val sessionId = registrationId // or generate a separate one
                        launch {
                            PeerEventManager.emitRegistrationSuccess()
                        }

                        call.respond(HttpStatusCode.OK, PeerResponse(sessionId))
                    }
                    post("/api/v1/prepare-upload") {
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

                        val accepted = PeerEventManager.emitPrepareUploadRequest(request)

                        if (accepted) {
                            val transmissionId = UUID.randomUUID().toString()
                            call.respond(
                                HttpStatusCode.OK,
                                PeerPrepareUploadResponse(transmissionId)
                            )
                        } else {
                            call.respond(HttpStatusCode.Forbidden, "Transfer rejected by receiver")
                        }
//                        val transmissionId = UUID.randomUUID().toString()
//                        call.respond(
//                            HttpStatusCode.OK,
//                            PeerPrepareUploadResponse(transmissionId)
//                        )
                    }
                }

            }
        }).start(wait = false)
    }

    override fun stop() {
        engine?.stop(1000, 5000)
    }
}
