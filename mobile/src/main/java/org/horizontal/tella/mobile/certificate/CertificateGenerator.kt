package org.horizontal.tella.mobile.certificate

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.OperatorCreationException
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

object CertificateGenerator {

    init {
        // Add BC provider only if not already registered
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
            println("Bouncy Castle provider registered successfully")
        } else {
            println("Bouncy Castle provider already registered")
        }

        // Debug: Print available signature algorithms
        val algorithms = Security.getAlgorithms("Signature")
        println("Available signature algorithms: ${algorithms.joinToString()}")
    }

    fun generateCertificate(
        commonName: String = "Tella Android",
        organization: String = "Tella",
        validityDays: Int = 365,
        ipAddress: String
    ): Pair<KeyPair, X509Certificate> {
        // Generate key pair
        val keyGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val now = Date()
        val validTo = Date(now.time + validityDays * 86400000L)

        val issuer = X500Name("CN=$commonName,O=$organization")
        val subject = issuer

        val serialNumber = CertificateUtils.generateSerialNumber()

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer, serialNumber, now, validTo, subject, keyPair.public
        )

        if (ipAddress.isNotEmpty()) {
            val san = org.bouncycastle.asn1.x509.GeneralNames(
                org.bouncycastle.asn1.x509.GeneralName(
                    org.bouncycastle.asn1.x509.GeneralName.iPAddress, ipAddress
                )
            )
            certBuilder.addExtension(
                org.bouncycastle.asn1.x509.Extension.subjectAlternativeName,
                false,
                san
            )
        }

        // Try both uppercase and lowercase variants of the algorithm name
        val signer = try {
            JcaContentSignerBuilder("SHA256WITHRSA")  // First try uppercase
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keyPair.private)
        } catch (e: OperatorCreationException) {
            try {
                JcaContentSignerBuilder("SHA256withRSA")  // Fallback to lowercase
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(keyPair.private)
            } catch (e2: OperatorCreationException) {
                throw IllegalStateException("Failed to create signer. Available algorithms: " +
                        Security.getAlgorithms("Signature").joinToString(), e2)
            }
        }

        val holder = certBuilder.build(signer)

        val certificate = JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(holder)

        return Pair(keyPair, certificate)
    }
}