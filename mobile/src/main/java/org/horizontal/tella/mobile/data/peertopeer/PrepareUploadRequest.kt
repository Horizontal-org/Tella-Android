package org.horizontal.tella.mobile.data.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class PrepareUploadRequest(
    val title: String,
    val sessionId: String,
    val files: List<FileItem>
)