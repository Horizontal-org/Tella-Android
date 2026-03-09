package org.horizontal.tella.mobile.data.repository

import com.hzontal.tella_vault.VaultFile

class ChunkableMediaFileRequestBody(vaultFile: VaultFile, skipBytes: Long, private val chunkLength: Long, progressListener: ProgressListener? = null) : SkippableMediaFileRequestBody(vaultFile, skipBytes, progressListener) {
    override fun contentLength(): Long = chunkLength

    override fun writeTo(sink: okio.BufferedSink) {
        getPublicInputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var totalRead = 0L
            while (totalRead < chunkLength) {
                val toRead = minOf(buffer.size.toLong(), chunkLength - totalRead).toInt()
                val read = inputStream.read(buffer, 0, toRead)
                if (read == -1) break
                sink.write(buffer, 0, read)
                totalRead += read
            }
        }
    }

    fun skipBytes(): Long {
        return skip
    }
    fun chunkSize(): Long {
        return chunkLength
    }
    fun totalBytes() : Long {
        return mediaFile.size
    }
}