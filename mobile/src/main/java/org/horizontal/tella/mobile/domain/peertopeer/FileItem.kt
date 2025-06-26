package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class P2PFile(
    val id: String,
    val fileName: String,
    val size: Long,
    val fileType: String,
    val sha256: String
)
