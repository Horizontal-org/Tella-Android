package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PeerRegisterPayload(
    val alias: String,
    val version: String = "2.0",
    val deviceModel: String,
    val deviceType: String = "mobile",
    val fingerprint: String,
    val port: Int = 53317,
    val protocol: String = "https",
    val download: Boolean = true,
    val pin: String,
    val nonce: String = UUID.randomUUID().toString()
)