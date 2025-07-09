package org.horizontal.tella.mobile.data.peertopeer.model

import org.horizontal.tella.mobile.domain.peertopeer.P2PFile

// --- ENUMS ---

enum class P2PFileStatus {
    QUEUE,
    SENDING,
    FAILED,
    FINISHED
}

enum class SessionStatus {
    WAITING,
    SENDING,
    FINISHED,
    FINISHED_WITH_ERRORS
}

data class ReceivingFile(
    var file: P2PFile,
    var status: P2PFileStatus = P2PFileStatus.QUEUE,
    var transmissionId: String? = null,
    var path: String? = null,
    var bytesReceived: Int = 0
)

class P2PSession(
    var sessionId: String,
    var status: SessionStatus = SessionStatus.WAITING,
    var files: MutableMap<String, ReceivingFile> = mutableMapOf(),
    var title: String? = null
) {
    val isActive: Boolean
        get() = status == SessionStatus.WAITING || status == SessionStatus.SENDING

    val hasFiles: Boolean
        get() = files.isNotEmpty()
}

class P2PServerState(
    var pin: String? = null,
    var session: P2PSession? = null,
    private var failedAttempts: Int = 0,
    //TODO ASK DHEKRA ABOIUT THIS
    private var isUsingManualConnection: Boolean = false
) {
    private val maxFailedAttempts = 3

    val hasReachedMaxAttempts: Boolean
        get() = failedAttempts >= maxFailedAttempts

    fun incrementFailedAttempts() {
        failedAttempts++
    }

    fun reset() {
        pin = null
        session = null
        failedAttempts = 0
        isUsingManualConnection = false
    }

    fun getFailedAttempts(): Int = failedAttempts
}
