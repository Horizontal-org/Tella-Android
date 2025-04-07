package org.horizontal.tella.mobile.data.peertopeer

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import org.horizontal.tella.mobile.domain.peertopeer.TellaServer
import java.security.KeyPair
import java.security.KeyStore
import java.security.cert.X509Certificate

const val port = 53317

class TellaHttpServer(
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
                        call.respondText("The server is running securely over HTTPS.")
                    }

                    // POST endpoint to handle device registration from peers
                    post("/api/register") {
                        try {
                            // Build a JSON response with device metadata
                            val response = buildJsonObject {
                                put("deviceType", "mobile") // Type of device (mobile/desktop)
                                put("version", "2.0")       // Protocol version
                                put("fingerprint", CertificateUtils.getPublicKeyHash(certificate)) // Unique cert fingerprint
                                put("port", serverPort)     // Port the server is listening on
                                put("protocol", "https")    // Connection protocol
                                put("download", true)       // Indicates if the device can receive files
                                put("deviceModel", android.os.Build.MODEL ?: "Android") // Device model name
                                put("alias", "AndroidDevice") // Friendly device name (can be customized later)
                            }

                            // Respond with the device info as JSON
                            call.respondText(response.toString(), ContentType.Application.Json)

                        } catch (e: Exception) {
                            // Catch and return any unexpected errors as JSON
                            call.respondText(
                                "Error occurred: ${e.localizedMessage}",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.InternalServerError
                            )
                        }
                    }
                }
            }
        }).start(wait = false)
    }

    override fun stop() {
        engine?.stop(1000, 5000)
    }
}
