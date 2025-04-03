package org.horizontal.tella.mobile.data.peertopeer

import com.google.gson.annotations.SerializedName

data class PeerConnectionPayload(
    @SerializedName("connect_code")
    val connectCode: String,

    @SerializedName("port")
    val port: Int,

    @SerializedName("certificate_hash")
    val certificateHash: String,

    @SerializedName("pin")
    val pin: String
)

