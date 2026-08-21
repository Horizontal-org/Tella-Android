package org.horizontal.tella.mobile.certificate

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

object CertificateGenerator {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateCertificate(
        commonName: String = "Tella Android",
        organization: String = "Tella",
        validityDays: Int = 7,
        ipAddress: String
    ): Pair<KeyPair, X509Certificate> {
        require(ipAddress.isNotBlank()) { "IP address must not be empty when generating a certificate." }

        val keyGen = KeyPairGenerator.getInstance("RSA")
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

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.private)

        val holder = certBuilder.build(signer)

        val certificate = JcaX509CertificateConverter()
            .getCertificate(holder)

        return Pair(keyPair, certificate)
    }


}