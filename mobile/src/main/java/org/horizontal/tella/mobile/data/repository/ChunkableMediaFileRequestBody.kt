package org.horizontal.tella.mobile.data.repository

import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.data.reports.utils.ChunkableMediaFileConfig

class ChunkableMediaFileRequestBody(vaultFile: VaultFile, skipBytes: Long, private val chunkLength: Long, progressListener: ProgressListener? = null) : SkippableMediaFileRequestBody(vaultFile, skipBytes, progressListener) {
    override fun contentLength(): Long = chunkLength

    override fun writeTo(sink: okio.BufferedSink) {
        getPublicInputStream().use { inputStream ->
            val buffer = ByteArray(BUFFER_SIZE)
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

    val config: ChunkableMediaFileConfig by lazy {
        val endByte = skip + chunkLength - 1
        ChunkableMediaFileConfig(
            skip,
            chunkLength,
            mediaFile.size,
            endByte
        )
    }

    companion object {
        private const val BUFFER_SIZE = 8192
    }
}
