package org.horizontal.tella.mobile.data.peertopeer.model

class P2PSession(
    var status: SessionStatus = SessionStatus.WAITING,
    var files: MutableMap<String, ProgressFile> = mutableMapOf(),
    var title: String? = null
) {
    val isActive: Boolean
        get() = status == SessionStatus.WAITING || status == SessionStatus.SENDING

    val hasFiles: Boolean
        get() = files.isNotEmpty()
}