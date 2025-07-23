package org.horizontal.tella.mobile.data.peertopeer.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerServer
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import java.security.KeyPair
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeerServerStarterManager @Inject constructor(
    private val peerToPeerManager: PeerToPeerManager
) {
    private var server: TellaPeerToPeerServer? = null

    fun startServer(
        ip: String,
        keyPair: KeyPair,
        pin: String,
        cert: X509Certificate,
        config: KeyStoreConfig,
        p2PSharedState: P2PSharedState,
    ) {
        if (isRunning()) {
            stopServerBlocking() // Ensure clean restart
        }

        server = TellaPeerToPeerServer(
            ip = ip,
            keyPair = keyPair,
            pin = pin,
            certificate = cert,
            keyStoreConfig = config,
            peerToPeerManager = peerToPeerManager,
            p2PSharedState = p2PSharedState
        )

        try {
            server?.start()
        } catch (e: Exception) {
            e.printStackTrace() // Optional: log to Sentry or Crashlytics
            server = null
        }
    }

    fun stopServer() {
        CoroutineScope(Dispatchers.IO).launch {
            stopServerBlocking()
        }
    }

    private fun stopServerBlocking() {
        try {
            server?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            server = null
        }
    }

    private fun isRunning(): Boolean = server != null
}
