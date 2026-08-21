package org.horizontal.tella.mobile.data.peertopeer

import io.ktor.server.application.ApplicationCall
import io.ktor.server.netty.NettyApplicationCall
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingPipelineCall
import io.netty.handler.ssl.SslHandler
import org.horizontal.tella.mobile.certificate.CertificateUtils
import timber.log.Timber
import java.security.cert.X509Certificate

/** Inside route handlers Ktor 3 wraps the engine call in [RoutingCall]; unwrap to reach Netty. */
private fun ApplicationCall.nettyCall(): NettyApplicationCall? {
    val unwrapped = when (this) {
        is RoutingCall -> pipelineCall.engineCall
        is RoutingPipelineCall -> engineCall
        else -> this
    }
    return unwrapped as? NettyApplicationCall
}

fun ApplicationCall.peerClientLeafCertificate(): X509Certificate? {
    val nettyCall = nettyCall()
    if (nettyCall == null) {
        Timber.w("P2P server: cannot unwrap %s to NettyApplicationCall", this::class.simpleName)
        return PeerMtlsSsl.peekPendingServerClientCert()
    }
    val channel = nettyCall.context.channel()
    channel.attr(PEER_CLIENT_LEAF_CERT_KEY).get()?.let { return it }

    val sslHandler = channel.pipeline().get(SslHandler::class.java)
    if (sslHandler != null) {
        val engine = sslHandler.engine()
        PeerMtlsSsl.peekCapturedServerClientCert(engine)?.let { return it }
        runCatching {
            engine.session.peerCertificates
                ?.firstOrNull { it is X509Certificate } as? X509Certificate
        }.getOrNull()?.let { return it }
    }

    PeerMtlsSsl.peekPendingServerClientCert()?.let {
        channel.attr(PEER_CLIENT_LEAF_CERT_KEY).compareAndSet(null, it)
        return it
    }

    Timber.w("P2P server: no client leaf cert on channel (sslHandler=%s)", sslHandler != null)
    return null
}

fun ApplicationCall.peerClientCertificateHashHex(): String? =
    peerClientLeafCertificate()?.let { CertificateUtils.getLeafCertificateDerSha256Hex(it) }
