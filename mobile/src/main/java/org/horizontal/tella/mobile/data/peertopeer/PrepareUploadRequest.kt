package org.horizontal.tella.mobile.data.peertopeer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrepareUploadRequest(
    val title: String,
    @SerialName("sessionId") val sessionId: String,
    val files: List<P2PFile>
)