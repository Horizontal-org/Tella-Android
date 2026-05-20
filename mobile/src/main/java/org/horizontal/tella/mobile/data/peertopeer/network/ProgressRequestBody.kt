package org.horizontal.tella.mobile.data.peertopeer.network

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

class ProgressRequestBody(
    private val inputStream: InputStream,
    private val contentLength: Long,
    private val onProgress: (Long, Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = "application/octet-stream".toMediaTypeOrNull()

    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded = 0L
        var read: Int

        while (inputStream.read(buffer).also { read = it } != -1) {
            sink.write(buffer, 0, read)
            uploaded += read
            onProgress(uploaded, contentLength)
        }
    }
}
