package org.horizontal.tella.mobile.data.peertopeer.remote

/**
 * Result of the manual-flow ping: the receiver leaf cert hash extracted from the TLS handshake, plus
 * the `senderShowHash` flag parsed from the ping response body (protocol §3.1).
 *
 * Per the protocol security note, [senderShowHash] must only be acted on AFTER the receiver hash has
 * been verified (the channel isn't authenticated until then).
 */
data class PeerPingResult(
    val receiverHash: String,
    val senderShowHash: Boolean,
)
