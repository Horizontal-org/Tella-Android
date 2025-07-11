package org.horizontal.tella.mobile.data.peertopeer.model

data class P2PSession(
    var status: SessionStatus = SessionStatus.WAITING,
    var files: MutableMap<String, ProgressFile> = mutableMapOf(),
    var title: String? = null
)