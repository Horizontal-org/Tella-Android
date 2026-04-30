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

    /**
     * SHA-256 of the leaf certificate DER ([X509Certificate.encoded]), lowercase hex.
     * Same value as [org.horizontal.tella.mobile.data.peertopeer.FingerprintResult.certHex].
     */
    fun getLeafCertificateDerSha256Hex(certificate: X509Certificate): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certificate.encoded)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Trust manager that accepts only servers whose leaf certificate DER hash matches [expectedLeafCertSha256Hex].
     * This allows secure pinning for self-signed peers without trusting arbitrary certificates.
     */
    fun getLeafCertPinnedTrustManager(expectedLeafCertSha256Hex: String): X509TrustManager {
        val expected = normalizeHex(expectedLeafCertSha256Hex)
        return object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val leaf = chain?.firstOrNull()
                    ?: throw CertificateException("Empty certificate chain")
                leaf.checkValidity()
                val actual = normalizeHex(getLeafCertificateDerSha256Hex(leaf))
                if (actual != expected) {
                    throw CertificateException("Leaf certificate hash mismatch")
                }
            }
        }
    }

    /**
     * Bootstrap trust manager for first-contact fingerprint collection:
     * it requires a present, currently-valid X.509 leaf cert, but does not require CA-chain trust.
     */
    fun getFingerprintCollectionTrustManager(): X509TrustManager =
        object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val leaf = chain?.firstOrNull()
                    ?: throw CertificateException("Empty certificate chain")
                leaf.checkValidity()
            }
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
     * SSLContext for first-contact fingerprint collection against self-signed peers.
     */
    fun getFingerprintCollectionSSLContext(): SSLContext {
        val tm = getFingerprintCollectionTrustManager()
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(tm), SecureRandom())
        }
    }

    private fun normalizeHex(hexLike: String): String =
        hexLike.trim().replace(":", "").replace("\\s".toRegex(), "").lowercase()
}