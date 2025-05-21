package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class PeerPrepareUploadResponse(
    val transmissionId: String
)