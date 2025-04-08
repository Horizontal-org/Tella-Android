package org.horizontal.tella.mobile.data.peertopeer.mapper

import android.os.Build
import org.horizontal.tella.mobile.certificate.CertificateUtils
import org.horizontal.tella.mobile.domain.peertopeer.PeerDeviceInfo
import java.security.cert.X509Certificate

object PeerDeviceInfoMapper {
    fun enrichWithServerInfo(
        request: PeerDeviceInfo,
        certificate: X509Certificate,
        port: Int
    ): PeerDeviceInfo {
        return request.copy(
            fingerprint = CertificateUtils.getPublicKeyHash(certificate),
            port = port,
            protocol = "https",
            deviceModel = Build.MODEL ?: "Android",
            alias = "AndroidDevice"
        )
    }
}