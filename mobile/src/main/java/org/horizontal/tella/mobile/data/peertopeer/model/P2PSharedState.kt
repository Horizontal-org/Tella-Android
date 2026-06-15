package org.horizontal.tella.mobile.data.peertopeer.model

class P2PSharedState(
    var ip: String = "",
    var port: String = "",
    /** Pinned receiver (server) certificate hash — used by sender for TLS pinning. */
    var hash: String = "",
    var pin: String? = null,
    var session: P2PSession? = null,
    private var failedAttempts: Int = 0,
    var isUsingManualConnection: Boolean = false,
    /** SHA-256 hex of this device's sender (client) certificate for the session. */
    var localSenderHash: String = "",
    /** SHA-256 hex of this device's receiver (server) certificate for the session. */
    var localReceiverHash: String = "",
    /** Sender-side pin of receiver certificate (same as [hash] once step 1 completes). */
    var pinnedReceiverHash: String = "",
    /** Receiver-side pin of sender certificate after step 2. */
    var pinnedSenderHash: String = "",
    var connectionPhase: P2PConnectionPhase = P2PConnectionPhase.IDLE,
    var receiverCanScanQr: Boolean = true,
    var senderCanScanQr: Boolean = true,
    /** From receiver QR — desktop receivers skip sender QR in step 2. */
    var senderShowHash: Boolean = false,
    /** Recipient confirmed their own server cert hash (flow D step 1). */
    var receiverHashConfirmed: Boolean = false,
    /** Recipient confirmed sender cert hash (flow D step 2). */
    var senderHashConfirmed: Boolean = false,
    var activeVerificationStep: P2PVerificationStep? = null,
) {

    companion object {
        fun Companion.createNewSession(): P2PSession {
            return P2PSession(
                sessionId = "",
                title = "",
                files = mutableMapOf(),
                status = SessionStatus.SENDING
            )
        }
    }

    fun pinReceiverHash(receiverHash: String) {
        pinnedReceiverHash = receiverHash
        hash = receiverHash
        connectionPhase = P2PConnectionPhase.RECEIVER_PINNED
    }

    fun pinSenderHash(senderHash: String) {
        pinnedSenderHash = senderHash
        if (connectionPhase == P2PConnectionPhase.RECEIVER_PINNED ||
            connectionPhase == P2PConnectionPhase.IDLE
        ) {
            connectionPhase = P2PConnectionPhase.MTLS_ESTABLISHED
        }
    }

    fun markRegistered() {
        connectionPhase = P2PConnectionPhase.REGISTERED
    }

    fun clear() {
        ip = ""
        port = ""
        hash = ""
        pin = null
        session = null
        failedAttempts = 0
        isUsingManualConnection = false
        localSenderHash = ""
        localReceiverHash = ""
        pinnedReceiverHash = ""
        pinnedSenderHash = ""
        connectionPhase = P2PConnectionPhase.IDLE
        receiverCanScanQr = true
        senderCanScanQr = true
        senderShowHash = false
        receiverHashConfirmed = false
        senderHashConfirmed = false
        activeVerificationStep = null
    }
}
