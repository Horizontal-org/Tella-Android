package org.horizontal.tella.mobile.domain.peertopeer

data class UploadProgressEvent(
    val fileId: String,
    val bytesWritten: Long,
    val totalBytes: Long
)