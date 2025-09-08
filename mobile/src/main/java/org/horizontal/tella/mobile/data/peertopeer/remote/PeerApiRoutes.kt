package org.horizontal.tella.mobile.data.peertopeer.remote

object PeerApiRoutes {
    const val REGISTER = "/api/v1/register"
    const val PREPARE_UPLOAD = "/api/v1/prepare-upload"
    const val UPLOAD = "/api/v1/upload"
    const val PING = "/api/v1/ping"
    const val CLOSE = "/api/v1/close-connection"


    fun buildUrl(ip: String, port: String, endpoint: String, secure: Boolean = true): String {
        val scheme = if (secure) "https" else "http"
        // Ensure endpoint starts with "/"
        val normalized = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        return "$scheme://$ip:$port$normalized"
    }


    fun buildUploadUrl(ip: String, port: String, sessionId: String, fileId: String, transmissionId: String): String {
        val baseUrl = buildUrl(ip, port, UPLOAD)
        return "$baseUrl?sessionId=$sessionId&fileId=$fileId&transmissionId=$transmissionId"
    }

}
