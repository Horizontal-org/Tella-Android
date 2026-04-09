package org.horizontal.tella.mobile.data.peertopeer

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.request.uri

/**
 * Mirrors Tella-Desktop `RateLimitingWare.Handler` (`backend/core/modules/server/service.go`):
 * optional [X-Real-Ip] only when the peer address is IPv4 loopback `127.0.0.1`.
 */
fun ApplicationCall.peerClientIpForRateLimit(): String {
    val host = request.local.remoteHost
    if (host == "127.0.0.1") {
        request.headers["X-Real-Ip"]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return host
}

fun ApplicationCall.peerUrlKeyForRateLimit(config: PeerServerRateLimitConfig): String =
    if (config.useFullUriAsKey) request.uri else request.path()
