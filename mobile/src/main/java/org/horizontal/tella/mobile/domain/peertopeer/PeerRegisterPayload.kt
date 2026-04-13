package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class PeerRegisterPayload(
    val pin: String,
    /** Optional on the wire like iOS `RegisterRequest`; sender always supplies a value. */
    val nonce: String? = null,
) {
    companion object {
        val EMPTY = PeerRegisterPayload(pin = "", nonce = null)
    }
}