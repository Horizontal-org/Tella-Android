package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable

/**
 * Body of `POST /api/v2/ping` (protocol §3.1). `senderShowHash` tells the manual sender whether it
 * must run sender-hash verification after register: true when the receiver has not already pinned the
 * sender certificate (e.g. it could not scan the sender QR — flow D), false when it has (flow C).
 *
 * Field name is camelCase to match the protocol doc and the iOS `PingResponse`.
 */
@Serializable
data class PeerPingResponse(
    val senderShowHash: Boolean,
)
