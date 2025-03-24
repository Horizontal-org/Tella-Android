package org.horizontal.tella.mobile.domain.entity.peertopeer

data class QRCodeInfos(
    val ipAddress: String,
    val pin: String,
    val hash: String
) {
    // Custom equality based only on ipAddress
    override fun equals(other: Any?): Boolean {
        return (other as? QRCodeInfos)?.ipAddress == ipAddress
    }

    override fun hashCode(): Int {
        return ipAddress.hashCode()
    }
}
