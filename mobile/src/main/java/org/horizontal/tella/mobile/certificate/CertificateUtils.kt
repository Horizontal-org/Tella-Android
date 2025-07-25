package org.horizontal.tella.mobile.certificate

import android.util.Base64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.KeyStore
import java.util.Date
import java.util.UUID
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object CertificateUtils {


    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    fun generateSelfSignedCertificate(keyPair: KeyPair, ipAddress: String): X509Certificate {
        val now = Date()
        val until = Date(now.time + 365L * 24 * 60 * 60 * 1000) // Valid for 1 year

        val serial = BigInteger(128, SecureRandom())
        val dn = X500Name("CN=Tella P2P, O=Tella, C=US")

        val certBuilder = JcaX509v3CertificateBuilder(
            dn, serial, now, until, dn, keyPair.public
        )

        val san = GeneralNames(
            GeneralName(GeneralName.iPAddress, ipAddress)
        )

        certBuilder.addExtension(
            Extension.subjectAlternativeName, false, san
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.private)

        val certHolder = certBuilder.build(signer)
        return JcaX509CertificateConverter().setProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
            .getCertificate(certHolder)
    }


    fun generateSerialNumber(): BigInteger {
        return BigInteger(128, SecureRandom())
    }

    fun certificateToPem(certificate: X509Certificate): String {
        val encoded = Base64.encodeToString(certificate.encoded, Base64.NO_WRAP)
        return "-----BEGIN CERTIFICATE-----\n$encoded\n-----END CERTIFICATE-----"
    }

    fun getPublicKeyHash(certificate: X509Certificate): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certificate.encoded)
        return hash.joinToString("") { "%02x".format(it) } // <-- fix here
    }


}