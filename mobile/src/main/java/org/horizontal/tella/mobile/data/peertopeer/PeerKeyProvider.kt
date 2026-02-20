package org.horizontal.tella.mobile.data.peertopeer

import org.horizontal.tella.mobile.certificate.CertificateUtils
import java.security.KeyPair
import java.security.cert.X509Certificate

object PeerKeyProvider {
    private var keyPair: KeyPair? = null
    private var certificate: X509Certificate? = null

    fun getKeyPair(): KeyPair {
        if (keyPair == null) {
            keyPair = CertificateUtils.generateKeyPair()
        }
        return keyPair!!
    }

    fun getCertificate(ipAddress: String): X509Certificate {
        if (certificate == null) {
            certificate = CertificateUtils.generateSelfSignedCertificate(getKeyPair(), ipAddress)
        }
        return certificate!!
    }

    fun reset() {
        keyPair = null
        certificate = null
    }
}

