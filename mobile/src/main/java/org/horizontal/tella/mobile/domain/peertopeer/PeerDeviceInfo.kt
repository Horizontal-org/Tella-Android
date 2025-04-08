package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class PeerDeviceInfo(
    val deviceType: String,
    val version: String,
    val fingerprint: String,
    val port: Int,
    val protocol: String,
    val download: Boolean,
    val deviceModel: String,
    val alias: String,
    val pin: String? = null,      // optional
    val nonce: String? = null     // optional
)