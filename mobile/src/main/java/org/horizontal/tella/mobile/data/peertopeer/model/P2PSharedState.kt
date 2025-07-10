package org.horizontal.tella.mobile.data.peertopeer.model

class P2PSharedState(
    var ip: String = "",
    var port: String = "",
    var sessionId: String = "",
    //THE HASH IS THE SAME EXPECTED FINGER PRINT
    var hash: String = "",
    var pin: String? = null,
    var session: P2PSession? = null,
    var failedAttempts: Int = 0,
    var isUsingManualConnection: Boolean = false
)
