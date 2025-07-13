package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class FileInfo(
    val id: String,
    val transmissionId: String
)
@Serializable
data class PeerPrepareUploadResponse(
    val files: List<FileInfo>
)