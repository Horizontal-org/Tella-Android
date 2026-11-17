package org.horizontal.tella.mobile.views.activity.viewer

import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.annotation.RequiresApi
import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.media.MediaFileHandler
import java.io.IOException
import java.io.InputStream

object VerificationFileResolution {
    @JvmStatic
    fun read(file: VaultFile): String? = try {
        when {
            file.mimeType?.startsWith("image/") == true -> {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                MediaFileHandler.getStream(file)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                format(options.outWidth, options.outHeight)
            }
            file.mimeType?.startsWith("video/") == true && Build.VERSION.SDK_INT >= 23 -> video(file)
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private fun format(width: Int, height: Int): String? =
        if (width > 0 && height > 0) "$width x ${height}px" else null

    @RequiresApi(23)
    private fun video(file: VaultFile): String? {
        val retriever = MediaMetadataRetriever()
        try {
            VaultMediaSource(file).use { source ->
                retriever.setDataSource(source)
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                return format(width ?: 0, height ?: 0)
            }
        } finally {
            retriever.release()
        }
    }

    @RequiresApi(23)
    private class VaultMediaSource(private val file: VaultFile) : MediaDataSource() {
        private var stream: InputStream? = null
        private var position = 0L

        @Synchronized
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (size == 0) return 0
            if (position < 0) throw IOException("Invalid media position")
            if (position >= file.size) return -1
            if (stream == null || position < this.position) {
                close()
                stream = MediaFileHandler.getStream(file) ?: throw IOException("Media unavailable")
                this.position = 0
            }
            val input = stream ?: throw IOException("Media unavailable")
            while (this.position < position) {
                val skipped = input.skip(position - this.position)
                if (skipped > 0) this.position += skipped
                else if (input.read() == -1) return -1
                else this.position++
            }
            val count = input.read(buffer, offset, size)
            if (count > 0) this.position += count
            return count
        }

        override fun getSize(): Long = file.size

        @Synchronized
        override fun close() {
            stream?.close()
            stream = null
        }
    }
}
