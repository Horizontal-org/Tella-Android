package org.horizontal.tella.mobile.data.peertopeer

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.SslHandshakeCompletionEvent
import io.netty.util.AttributeKey
import io.netty.util.concurrent.GenericFutureListener
import timber.log.Timber
import java.security.cert.X509Certificate

internal val PEER_CLIENT_LEAF_CERT_KEY: AttributeKey<X509Certificate> =
    AttributeKey.valueOf("peerClientLeafCert")

/** Stores the sender leaf certificate on the channel after mTLS handshake completes. */
internal class PeerClientCertCaptureHandler : ChannelInboundHandlerAdapter() {

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val sslHandler = ctx.pipeline().get(SslHandler::class.java) ?: return
        sslHandler.handshakeFuture().addListener(handshakeListener(ctx))
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is SslHandshakeCompletionEvent && evt.isSuccess) {
            storePeerLeaf(ctx, "handshake-event")
        }
        super.userEventTriggered(ctx, evt)
    }

    private fun handshakeListener(ctx: ChannelHandlerContext): GenericFutureListener<io.netty.util.concurrent.Future<in io.netty.channel.Channel>> =
        GenericFutureListener { future ->
            if (future.isSuccess) {
                storePeerLeaf(ctx, "handshake-future")
            } else {
                Timber.w(future.cause(), "P2P server mTLS handshake failed")
            }
        }

    private fun storePeerLeaf(ctx: ChannelHandlerContext, source: String) {
        if (ctx.channel().attr(PEER_CLIENT_LEAF_CERT_KEY).get() != null) return
        capturePeerLeaf(ctx)?.let { cert ->
            ctx.channel().attr(PEER_CLIENT_LEAF_CERT_KEY).set(cert)
            Timber.d("P2P server captured client leaf cert via %s", source)
        } ?: Timber.w("P2P server: mTLS handshake ok but no client leaf cert captured")
    }

    private fun capturePeerLeaf(ctx: ChannelHandlerContext): X509Certificate? {
        val sslHandler = ctx.pipeline().get(SslHandler::class.java) ?: return null
        val engine = sslHandler.engine()
        PeerMtlsSsl.peekCapturedServerClientCert(engine)?.let { return it }
        PeerMtlsSsl.peekPendingServerClientCert()?.let { return it }
        return runCatching {
            sslHandler.engine().session.peerCertificates?.firstOrNull() as? X509Certificate
        }.getOrElse { error ->
            Timber.w(error, "Failed to read peer client certificate from SSLSession")
            null
        }
    }
}
