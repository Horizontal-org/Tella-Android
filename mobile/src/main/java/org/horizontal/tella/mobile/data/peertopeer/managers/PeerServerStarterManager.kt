package org.horizontal.tella.mobile.data.peertopeer.managers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /** Last credentials the running server was started with (avoids redundant stop/start). */
    private var listeningIp: String? = null
    private var listeningPin: String? = null

    /**
     * Starts or restarts the embedded server so its PIN matches the QR payload.
     * If [ip] and [pin] match the already-running server, returns true without tearing down Netty
     * (avoids port bind races and duplicate work when [setupServerAndQr] is triggered twice).
     */
    @Synchronized
    fun startServer(
        ip: String,
        keyPair: KeyPair,
        pin: String,
        cert: X509Certificate,
        config: KeyStoreConfig,
        p2PSharedState: P2PSharedState,
        rateLimitConfig: PeerServerRateLimitConfig = PeerServerRateLimitConfig.DEFAULT,
    ): Boolean {
        if (server != null && listeningIp == ip && listeningPin == pin) {
            return true
        }
        val hadServer = server != null
        server?.let {
            try {
                it.stop()
            } catch (e: Exception) {
                Timber.e(e, "P2P embedded server stop before restart failed")
            }
        }
        server = null
        listeningIp = null
        listeningPin = null
        if (hadServer) {
            try {
                Thread.sleep(120)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        return try {
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
            listeningIp = ip
            listeningPin = pin
            true
        } catch (e: Exception) {
            Timber.e(e, "P2P embedded server failed to start")
            server = null
            listeningIp = null
            listeningPin = null
            false
        }
    }

    /** Prefer calling from a background thread; Netty shutdown can take a short time. */
    @Synchronized
    fun stopServer() {
        server?.let {
            try {
                it.stop()
            } catch (e: Exception) {
                Timber.e(e, "P2P embedded server stop failed")
            }
        }
        server = null
        listeningIp = null
        listeningPin = null
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
