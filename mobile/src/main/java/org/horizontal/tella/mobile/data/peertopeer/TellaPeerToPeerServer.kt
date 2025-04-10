package org.horizontal.tella.mobile.data.peertopeer

import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.horizontal.tella.mobile.domain.peertopeer.PeerResponse
import org.horizontal.tella.mobile.domain.peertopeer.TellaServer
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
    private val keyStoreConfig: KeyStoreConfig
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
                routing {
                    // Root route to confirm the server is running
                    get("/") {
                        Log.i("Test","Server started")
                        call.respondText("The server is running securely over HTTPS.")
                    }

                    // POST endpoint to handle device registration from peers
                    post("/api/v1/register") {
                        try {
                            val peerInfo = call.receive<PeerRegisterPayload>()
                            //val response = peerInfo.sessionId
                          //  call.respondText(response, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respondText(
                                "Error occurred: ${e.localizedMessage}",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.InternalServerError
                            )
                        }
                    }
                    post("/api/v1/prepare-upload") {
                        val request = try {
                            call.receive<PrepareUploadRequest>()
                        } catch (e: Exception) {
                            call.respondText("Invalid body", status = HttpStatusCode.BadRequest)
                            return@post
                        }

                        // Optionally validate fields
                        if (request.title.isBlank() || request.sessionId.isBlank() || request.files.isEmpty()) {
                            call.respondText("Missing required fields", status = HttpStatusCode.BadRequest)
                            return@post
                        }

                        // You can process the files or store metadata here

                        val transmissionId = UUID.randomUUID().toString()
                        val response = buildJsonObject {
                            put("transmissionId", transmissionId)
                        }

                        call.respondText(
                            response.toString(),
                            contentType = ContentType.Application.Json
                        )
                    }

                }

            }
        }).start(wait = false)
    }

    override fun stop() {
        engine?.stop(1000, 5000)
    }
}
