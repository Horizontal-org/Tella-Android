package org.horizontal.tella.mobile.data.peertopeer

import org.horizontal.tella.mobile.certificate.CertificateUtils
import java.security.KeyPair
import java.security.cert.X509Certificate

object PeerKeyProvider {
    private var receiverKeyPair: KeyPair? = null
    private var receiverCertificate: X509Certificate? = null
    private var receiverCertificateKey: String? = null

    private var senderKeyPair: KeyPair? = null
    private var senderCertificate: X509Certificate? = null

    fun getKeyPair(): KeyPair {
        if (receiverKeyPair == null) {
            receiverKeyPair = CertificateUtils.generateKeyPair()
        }
        return receiverKeyPair!!
    }

    fun getCertificate(ipAddress: String): X509Certificate =
        getCertificate(listOf(ipAddress))

    fun getCertificate(ipAddresses: List<String>): X509Certificate {
        val key = ipAddresses.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted().joinToString(",")
        require(key.isNotEmpty())
        if (receiverCertificate == null || receiverCertificateKey != key) {
            receiverCertificate = CertificateUtils.generateSelfSignedCertificate(getKeyPair(), ipAddresses)
            receiverCertificateKey = key
        }
        return receiverCertificate!!
    }

    fun ensureSenderIdentity(): Pair<KeyPair, X509Certificate> {
        if (senderKeyPair == null) {
            senderKeyPair = CertificateUtils.generateKeyPair()
            senderCertificate = CertificateUtils.generateSelfSignedClientCertificate(senderKeyPair!!)
        }
        return senderKeyPair!! to senderCertificate!!
    }

    fun getSenderCertificate(): X509Certificate = ensureSenderIdentity().second

    fun getSenderCertificateHash(): String =
        CertificateUtils.getLeafCertificateDerSha256Hex(getSenderCertificate())

    fun reset() {
        receiverKeyPair = null
        receiverCertificate = null
        receiverCertificateKey = null
        senderKeyPair = null
        senderCertificate = null
    }
}
