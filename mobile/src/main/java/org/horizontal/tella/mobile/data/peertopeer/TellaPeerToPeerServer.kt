package org.horizontal.tella.mobile.data.peertopeer

import android.util.Log
import com.google.gson.Gson
import io.ktor.http.ContentType
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.PeerPrepareUploadResponse
import org.horizontal.tella.mobile.domain.peertopeer.PeerRegisterPayload
import org.horizontal.tella.mobile.domain.peertopeer.PeerResponse
import org.horizontal.tella.mobile.domain.peertopeer.TellaServer
import java.security.KeyPair
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.UUID
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

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
                install(ContentNegotiation) {
                    json()
                }
                routing {
                    // Root route to confirm the server is running
                    get("/") {
                        Log.i("Test", "Server started")
                        call.respondText("The server is running securely over HTTPS.")
                    }

                    post("/api/v1/register") {
                        val request = try {
                            call.receive<PeerRegisterPayload>()
                        } catch (e: Exception) {
                            call.respondText(
                                """{"error": "Invalid request body"}""",
                                ContentType.Application.Json,
                                HttpStatusCode.BadRequest
                            )
                            return@post
                        }

                        val sessionId = UUID.randomUUID().toString()
                        call.respondText(
                            Gson().toJson(PeerResponse(sessionId)),
                            ContentType.Application.Json,
                            HttpStatusCode.OK
                        )
                    }


                    post("/api/v1/prepare-upload") {
                        val request = try {
                            call.receive<PrepareUploadRequest>()
                        } catch (e: Exception) {
                            call.respondText("Invalid body", status = HttpStatusCode.BadRequest)
                            return@post
                        }

                        if (request.title.isBlank() || request.sessionId.isBlank() || request.files.isEmpty()) {
                            call.respondText(
                                "Missing required fields",
                                status = HttpStatusCode.BadRequest
                            )
                            return@post
                        }

                        // we can process the files or store metadata here
                        val transmissionId = UUID.randomUUID().toString()
                        call.respondText(
                            Gson().toJson(PeerPrepareUploadResponse(transmissionId)),
                            ContentType.Application.Json,
                            HttpStatusCode.OK
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
