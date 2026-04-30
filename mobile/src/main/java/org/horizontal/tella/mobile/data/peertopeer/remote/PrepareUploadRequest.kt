package org.horizontal.tella.mobile.data.peertopeer.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.horizontal.tella.mobile.domain.peertopeer.P2PFile

@Serializable
data class PrepareUploadRequest(
    val title: String,
    @SerialName("sessionId") val sessionId: String,
    /** Optional like iOS `PrepareUploadRequest`; missing/null → replay check fails with conflict. */
    val nonce: String? = null,
    val files: List<P2PFile>
)