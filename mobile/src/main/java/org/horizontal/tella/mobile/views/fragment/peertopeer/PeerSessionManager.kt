package org.horizontal.tella.mobile.views.fragment.peertopeer

//TODO CHECK IF WE SHOULD REMOVE THIS
data class PeerConnectionInfo(
    val ip: String = "",
    val port: String = "",
    val expectedFingerprint: String = "",
    val sessionId: String = "",
    val hash: String = "",
    val pin: Int = 0,
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

}
