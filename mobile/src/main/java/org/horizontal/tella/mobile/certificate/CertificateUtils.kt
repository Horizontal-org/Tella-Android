package org.horizontal.tella.mobile.certificate

import android.util.Base64
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.KeyStore
import java.util.UUID
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object CertificateUtils {

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
        val hash = digest.digest(certificate.publicKey.encoded)
        return hash.joinToString("") { "%04x".format(it) }
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