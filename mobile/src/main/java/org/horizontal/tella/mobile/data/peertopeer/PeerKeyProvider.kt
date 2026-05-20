package org.horizontal.tella.mobile.data.peertopeer

import org.horizontal.tella.mobile.certificate.CertificateUtils
import java.security.KeyPair
import java.security.cert.X509Certificate

object PeerKeyProvider {
    private var keyPair: KeyPair? = null
    private var certificate: X509Certificate? = null
    private var certificateKey: String? = null

    fun getKeyPair(): KeyPair {
        if (keyPair == null) {
            keyPair = CertificateUtils.generateKeyPair()
        }
        return keyPair!!
    }

    fun getCertificate(ipAddress: String): X509Certificate =
        getCertificate(listOf(ipAddress))

    fun getCertificate(ipAddresses: List<String>): X509Certificate {
        val key = ipAddresses.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted().joinToString(",")
        require(key.isNotEmpty())
        if (certificate == null || certificateKey != key) {
            certificate = CertificateUtils.generateSelfSignedCertificate(getKeyPair(), ipAddresses)
            certificateKey = key
        }
        return certificate!!
    }

    fun reset() {
        keyPair = null
        certificate = null
        certificateKey = null
    }
}

