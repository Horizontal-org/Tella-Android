package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PeerRegisterPayload(
    val pin: String,
    val nonce: String = UUID.randomUUID().toString()
)