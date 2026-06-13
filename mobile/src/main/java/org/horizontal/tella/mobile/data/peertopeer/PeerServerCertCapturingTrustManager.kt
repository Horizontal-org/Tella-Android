package org.horizontal.tella.mobile.data.peertopeer

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Client-side wrapper that records the receiver (server) leaf certificate seen during the TLS
 * handshake. Needed because OkHttp's [okhttp3.Handshake.peerCertificates] can be empty on some
 * Android builds even after a successful handshake (SSLSession does not always expose peer certs
 * with custom trust managers), which would break the manual flow's receiver-hash bootstrap.
 */
internal class PeerServerCertCapturingTrustManager(
    private val delegate: X509TrustManager,
) : X509TrustManager {

    @Volatile
    var lastServerLeaf: X509Certificate? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
        delegate.checkClientTrusted(chain, authType)

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        delegate.checkServerTrusted(chain, authType)
        lastServerLeaf = chain?.firstOrNull()
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
}
