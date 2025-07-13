package org.horizontal.tella.mobile.data.peertopeer.model

class P2PSharedState(
    var ip: String = "",
    var port: String = "",
    var hash: String = "",
    var pin: String? = null,
    var session: P2PSession? = null,
    private var failedAttempts: Int = 0,
    var isUsingManualConnection: Boolean = false,
) {

    fun clear() {
        ip = ""
        port = ""
        hash = ""
        pin = null
        session = null
        failedAttempts = 0
        isUsingManualConnection = false
    }
}
