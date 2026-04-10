package org.horizontal.tella.mobile.data.peertopeer.managers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.data.peertopeer.PeerToPeerConstants
import org.horizontal.tella.mobile.data.peertopeer.PeerServerRateLimitConfig
import org.horizontal.tella.mobile.data.peertopeer.TellaPeerToPeerServer
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.domain.peertopeer.KeyStoreConfig
import java.io.File
import java.security.KeyPair
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class PeerServerStarterManager @Inject constructor(
    private val peerToPeerManager: PeerToPeerManager,
    @param:ApplicationContext private val appContext: Context,
) {
    private var server: TellaPeerToPeerServer? = null

    fun startServer(
        ip: String,
        keyPair: KeyPair,
        pin: String,
        cert: X509Certificate,
        config: KeyStoreConfig,
        p2PSharedState: P2PSharedState,
        rateLimitConfig: PeerServerRateLimitConfig = PeerServerRateLimitConfig.DEFAULT,
    ) {
        if (server == null) {
            try {
                val receiveDir = p2pReceiveDir().apply { mkdirs() }
                clearStaleReceiveFiles(receiveDir)
                server = TellaPeerToPeerServer(
                    ip = ip,
                    keyPair = keyPair,
                    pin = pin,
                    certificate = cert,
                    keyStoreConfig = config,
                    peerToPeerManager = peerToPeerManager,
                    p2PSharedState = p2PSharedState,
                    receiveDir = receiveDir,
                    rateLimitConfig = rateLimitConfig,
                )
                server?.start()
            } catch (e: Exception) {
                Timber.e(e, "P2P embedded server failed to start")
                server = null
            }
        }
    }

    fun stopServer() {
        CoroutineScope(Dispatchers.IO).launch {
            server?.stop()
            server = null
        }
    }

    fun isRunning(): Boolean = server != null

    private fun p2pReceiveDir(): File =
        File(appContext.cacheDir, PeerToPeerConstants.P2P_RECEIVE_SUBDIR)

    /** Orphans from crashed sessions; safe before a new server instance (no active handoff yet). */
    private fun clearStaleReceiveFiles(dir: File) {
        dir.listFiles()?.forEach { child ->
            if (child.isFile) child.delete()
        }
    }
}
