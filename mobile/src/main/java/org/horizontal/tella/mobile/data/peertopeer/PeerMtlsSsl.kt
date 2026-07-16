package org.horizontal.tella.mobile.data.peertopeer

import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import org.horizontal.tella.mobile.certificate.CertificateUtils
import java.security.KeyPair
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object PeerMtlsSsl {

    private val serverClientCertCaptor = PeerClientCertCapturingTrustManager()

    fun peekCapturedServerClientCert(engine: SSLEngine): X509Certificate? =
        serverClientCertCaptor.peekClientLeafForEngine(engine)

    fun peekPendingServerClientCert(): X509Certificate? =
        serverClientCertCaptor.peekPendingClientLeaf()

    fun createSenderKeyManagers(keyPair: KeyPair, certificate: X509Certificate) =
        createKeyManagers(keyPair, certificate)

    fun createSenderSslContext(
        senderKeyPair: KeyPair,
        senderCertificate: X509Certificate,
        pinnedReceiverHash: String?,
        trustManagerOverride: X509TrustManager? = null,
    ): SSLContext {
        val trustManager: X509TrustManager = trustManagerOverride
            ?: if (pinnedReceiverHash.isNullOrBlank()) {
                CertificateUtils.getFingerprintCollectionTrustManager()
            } else {
                CertificateUtils.getLeafCertPinnedTrustManager(pinnedReceiverHash)
            }
        return SSLContext.getInstance("TLS").apply {
            init(
                createKeyManagers(senderKeyPair, senderCertificate),
                arrayOf<TrustManager>(trustManager),
                SecureRandom(),
            )
        }
    }

    fun buildNettyServerSslContext(
        keyPair: KeyPair,
        certificate: X509Certificate,
    ): SslContext {
        return SslContextBuilder.forServer(keyPair.private, certificate)
            .sslProvider(SslProvider.JDK)
            .trustManager(serverClientCertCaptor)
            .clientAuth(ClientAuth.REQUIRE)
            .protocols("TLSv1.3", "TLSv1.2")
            .applicationProtocolConfig(
                ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_1_1,
                ),
            )
            .build()
    }

    private fun createKeyManagers(keyPair: KeyPair, certificate: X509Certificate) =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            val password = "peer".toCharArray()
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setKeyEntry(
                    "peer",
                    keyPair.private,
                    password,
                    arrayOf(certificate),
                )
            }
            init(keyStore, password)
        }.keyManagers
}
