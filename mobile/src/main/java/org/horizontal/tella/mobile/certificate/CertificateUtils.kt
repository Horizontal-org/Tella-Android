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

    fun generateSelfSignedCertificate(keyPair: KeyPair): X509Certificate {
        val certInfo = org.bouncycastle.x509.X509V3CertificateGenerator()
        val now = java.util.Date()
        val until = java.util.Date(now.time + 365L * 24 * 60 * 60 * 1000) // 1 year

        val dnName = org.bouncycastle.jce.X509Principal("CN=Tella P2P, O=Tella, C=US")

        certInfo.setSerialNumber(generateSerialNumber())
        certInfo.setIssuerDN(dnName)
        certInfo.setNotBefore(now)
        certInfo.setNotAfter(until)
        certInfo.setSubjectDN(dnName)
        certInfo.setPublicKey(keyPair.public)
        certInfo.setSignatureAlgorithm("SHA256WithRSAEncryption")

        return certInfo.generate(keyPair.private) as X509Certificate
    }

    fun generateSelfSignedCertificate(keyPair: KeyPair, ipAddress: String): X509Certificate {
        val now = Date()
        val until = Date(now.time + 365L * 24 * 60 * 60 * 1000) // Valid for 1 year

        val serial = BigInteger(128, SecureRandom())
        val dn = X500Name("CN=Tella P2P, O=Tella, C=US")

        val certBuilder = JcaX509v3CertificateBuilder(
            dn,
            serial,
            now,
            until,
            dn,
            keyPair.public
        )

        val san = GeneralNames(
            GeneralName(GeneralName.iPAddress, ipAddress)
        )

        certBuilder.addExtension(
            Extension.subjectAlternativeName,
            false,
            san
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .build(keyPair.private)

        val certHolder = certBuilder.build(signer)
        return JcaX509CertificateConverter()
            .setProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
            .getCertificate(certHolder)
    }



    fun generateSerialNumber(): BigInteger {
        return BigInteger(128, SecureRandom())
    }

    fun certificateToPem(certificate: X509Certificate): String {
        val encoded = Base64.encodeToString(certificate.encoded, Base64.NO_WRAP)
        return "-----BEGIN CERTIFICATE-----\n$encoded\n-----END CERTIFICATE-----"
    }

    fun pemToCertificate(pemString: String): X509Certificate {
        val cleanPem = pemString
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s".toRegex(), "")
        val decoded = Base64.decode(cleanPem, Base64.DEFAULT)
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(decoded.inputStream()) as X509Certificate
    }

    fun encodeBase64(data: ByteArray): String =
        Base64.encodeToString(data, Base64.NO_WRAP)

    fun decodeBase64(encodedString: String): ByteArray =
        Base64.decode(encodedString, Base64.NO_WRAP)


    fun getPublicKeyHash(certificate: X509Certificate): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certificate.encoded)
        return hash.joinToString("") { "%02x".format(it) } // <-- fix here
    }

    fun createSSLContext(
        keyPair: KeyPair,
        certificate: X509Certificate
    ): SSLContext {
        val keyStore = KeyStore.getInstance("PKCS12")
        val keyPassword =  UUID.randomUUID().toString()
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            "alias",
            keyPair.private,
            keyPassword.toCharArray(),
            arrayOf(certificate)
        )

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, keyPassword.toCharArray())

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, tmf.trustManagers, SecureRandom())

        return sslContext
    }

    fun getTrustManager(certificate: X509Certificate): X509TrustManager {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("alias", certificate)

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        return tmf.trustManagers[0] as X509TrustManager
    }
}