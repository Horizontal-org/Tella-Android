package org.horizontal.tella.mobile.data.reports.utils

class ChunkableMediaFileConfig(
    val skipBytes: Long,
    val chunkSize: Long,
    val totalBytes: Long,
    val endByte: Long
) {
    fun contentRange(): String {
        return "bytes $skipBytes-$endByte/$totalBytes"
    }
}
