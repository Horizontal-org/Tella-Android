package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class PeerRegisterPayload(
    val pin: String? = null,
    val nonce: String? = null,
) {
    companion object {
        val EMPTY = PeerRegisterPayload(pin = null, nonce = null)
    }
}