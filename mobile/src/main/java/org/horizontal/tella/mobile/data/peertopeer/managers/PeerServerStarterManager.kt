package org.horizontal.tella.mobile.data.peertopeer.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.data.peertopeer.TELLAPeerToPeerServer
import org.horizontal.tella.mobile.data.peertopeer.model.P2PServerState
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import java.security.KeyPair
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeerServerStarterManager @Inject constructor(
    private val peerToPeerManager: PeerToPeerManager
) {
    private var server: TELLAPeerToPeerServer? = null

    fun startServer(
        ip: String,
        keyPair: KeyPair,
        pin: String,
        cert: X509Certificate,
        config: KeyStoreConfig,
        p2PServerState: P2PServerState,
    ) {
        if (server == null) {
            server = TELLAPeerToPeerServer(
                ip = ip,
                keyPair = keyPair,
                pin = pin,
                certificate = cert,
                keyStoreConfig = config,
                peerToPeerManager = peerToPeerManager,
                p2PServerState = p2PServerState
            )
            server?.start()
        }
    }

    //TODO: AHLEM CHECK WHERE WE WANT TO STOP THE SERVER
    fun stopServer() {
        CoroutineScope(Dispatchers.IO).launch {
            server?.stop()
            server = null
        }
    }

    fun isRunning(): Boolean = server != null
}
