package org.horizontal.tella.mobile.data.peertopeer

import timber.log.Timber
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

/**
 * Trust manager used only to let self-signed peer certificates complete the TLS handshake
 * so we can capture the client certificate.
 *
 * This does NOT authorize the peer.
 * Authorization happens after the handshake by comparing the captured client certificate
 * hash against the QR-scanned pinned sender hash.
 *
 * Any request whose certificate is missing or does not match the pinned hash must be rejected
 * by the route handler.
 */
internal class PeerClientCertCapturingTrustManager : X509ExtendedTrustManager() {

    private val clientLeafByEngine = ConcurrentHashMap<SSLEngine, X509Certificate>()
    @Volatile
    private var pendingClientLeaf: X509Certificate? = null

    fun peekClientLeafForEngine(engine: SSLEngine): X509Certificate? =
        clientLeafByEngine[engine] ?: pendingClientLeaf

    fun peekPendingClientLeaf(): X509Certificate? = pendingClientLeaf

    fun consumePendingClientLeaf(): X509Certificate? =
        pendingClientLeaf.also { pendingClientLeaf = null }

    private fun rememberClient(engine: SSLEngine?, chain: Array<out X509Certificate>) {
        val leaf = chain.firstOrNull() ?: return
        pendingClientLeaf = leaf
        if (engine != null) {
            clientLeafByEngine[engine] = leaf
            Timber.d(
                "P2P mTLS trust manager captured client leaf (engine=%d)",
                System.identityHashCode(engine),
            )
        } else {
            Timber.d("P2P mTLS trust manager captured client leaf (no engine)")
        }
    }

    override fun checkClientTrusted(
        chain: Array<out X509Certificate>,
        authType: String,
        engine: SSLEngine,
    ) {
        Timber.d(
            "P2P mTLS checkClientTrusted(engine) authType=%s chain=%d subject=%s",
            authType, chain.size, chain.firstOrNull()?.subjectX500Principal,
        )
        rememberClient(engine, chain)
    }

    override fun checkClientTrusted(
        chain: Array<out X509Certificate>,
        authType: String,
        socket: Socket,
    ) {
        Timber.d(
            "P2P mTLS checkClientTrusted(socket) authType=%s chain=%d subject=%s",
            authType, chain.size, chain.firstOrNull()?.subjectX500Principal,
        )
        rememberClient(null, chain)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        Timber.d(
            "P2P mTLS checkClientTrusted(plain) authType=%s chain=%d subject=%s",
            authType, chain.size, chain.firstOrNull()?.subjectX500Principal,
        )
        rememberClient(null, chain)
    }

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>,
        authType: String,
        engine: SSLEngine,
    ) = Unit

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>,
        authType: String,
        socket: Socket,
    ) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
