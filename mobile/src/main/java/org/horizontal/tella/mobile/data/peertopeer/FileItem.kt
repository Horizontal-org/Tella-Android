package org.horizontal.tella.mobile.data.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    val id: String,
    val fileName: String,
    val size: Long,
    val fileType: String,
    val sha256: String
)