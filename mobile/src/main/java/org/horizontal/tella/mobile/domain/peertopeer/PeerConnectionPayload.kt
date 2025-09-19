package org.horizontal.tella.mobile.domain.peertopeer

import com.google.gson.annotations.SerializedName

data class PeerConnectionPayload(
    @SerializedName("ip_address")
    val ipAddress : String,

    @SerializedName("port")
    val port: Int,

    @SerializedName("certificate_hash")
    val certificateHash: String,

    @SerializedName("pin")
    val pin: String
)

