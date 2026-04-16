package org.horizontal.tella.mobile.certificate

import android.util.Base64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.KeyStore
import java.util.Date
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object CertificateUtils {


    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }


    fun generateSelfSignedCertificate(keyPair: KeyPair, ipAddresses: List<String>): X509Certificate {
        val uniqueIps = ipAddresses.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        require(uniqueIps.isNotEmpty()) { "At least one IP is required for the P2P certificate SAN." }

        val now = Date()
        val until = Date(now.time + 365L * 24 * 60 * 60 * 1000) // Valid for 1 year

        val serial = BigInteger(128, SecureRandom())
        val dn = X500Name("CN=Tella P2P, O=Tella, C=US")

        val certBuilder = JcaX509v3CertificateBuilder(
            dn, serial, now, until, dn, keyPair.public
        )

        val generalNames = uniqueIps.map { GeneralName(GeneralName.iPAddress, it) }.toTypedArray()
        val san = GeneralNames(generalNames)

        certBuilder.addExtension(
            Extension.subjectAlternativeName, false, san
        )
        certBuilder.addExtension(
            Extension.basicConstraints, true, org.bouncycastle.asn1.x509.BasicConstraints(false)
        )
        certBuilder.addExtension(
            Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
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

    /**
     * Returns the system default X509TrustManager (validates against device CA store).
     * Use this for all TLS connections instead of trust-all to satisfy CVE-2025-TELLA3-001.
     */
    fun getDefaultTrustManager(): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val trustManagers = tmf.trustManagers
        require(trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            "Unexpected default trust managers: ${trustManagers.contentToString()}"
        }
        return trustManagers[0] as X509TrustManager
    }

    /**
     * Returns an SSLContext initialized with the system default TrustManager.
     */
    fun getDefaultSSLContext(): SSLContext {
        val tm = getDefaultTrustManager()
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(tm), SecureRandom())
        }
    }
}