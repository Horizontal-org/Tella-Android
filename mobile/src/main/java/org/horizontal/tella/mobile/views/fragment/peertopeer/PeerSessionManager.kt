package org.horizontal.tella.mobile.views.fragment.peertopeer

/**
 * Created by wafa on 2/6/2025.
 */

object PeerSessionManager {
    var ip: String? = null
    var port: String? = null
    var hash: String? = null
    var sessionId: String? = null

    fun isSessionValid(): Boolean {
        return !ip.isNullOrEmpty() &&
                !port.isNullOrEmpty() &&
                !hash.isNullOrEmpty() &&
                !sessionId.isNullOrEmpty()
    }

    fun clear() {
        ip = null
        port = null
        hash = null
        sessionId = null
    }
}