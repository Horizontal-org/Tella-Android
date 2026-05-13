package org.horizontal.tella.mobile.data.peertopeer

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path


fun ApplicationCall.peerClientIpForRateLimit(): String {
    val host = request.local.remoteHost
    if (host == "127.0.0.1") {
        request.headers["X-Real-Ip"]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return host
}

/** Request path (no query) for per-route rate limiting, e.g. `/api/v1/upload`. */
fun ApplicationCall.peerRoutePathForRateLimit(): String = request.path()
