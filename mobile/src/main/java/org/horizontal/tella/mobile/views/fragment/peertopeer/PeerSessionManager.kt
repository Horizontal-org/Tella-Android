package org.horizontal.tella.mobile.views.fragment.peertopeer

/**
 * Created by wafa on 2/6/2025.
 */

data class PeerConnectionInfo(
    val ip: String = "",
    val port: String = "",
    val expectedFingerprint: String = "",
    val sessionId: String = "",
    val hash: String = "",
    val pin: Int = 0,
    var transmissionId: String? = null
)

object PeerSessionManager {
    private var connectionInfo: PeerConnectionInfo? = null

    fun saveConnectionInfo(
        ip: String,
        port: String,
        expectedFingerprint: String,
        sessionId: String
    ) {
        connectionInfo = PeerConnectionInfo(ip, port, expectedFingerprint, sessionId)
    }

    fun getConnectionInfo(): PeerConnectionInfo? = connectionInfo

    fun isSessionValid(): Boolean = connectionInfo != null

    private val transmissionMap = mutableMapOf<String, String>() // fileId -> transmissionId

    fun saveTransmissionId(fileId: String, transmissionId: String) {
        transmissionMap[fileId] = transmissionId
    }

    fun getTransmissionId(fileId: String): String? {
        return transmissionMap[fileId]
    }

    fun clearTransmissions() {
        transmissionMap.clear()
    }
}
