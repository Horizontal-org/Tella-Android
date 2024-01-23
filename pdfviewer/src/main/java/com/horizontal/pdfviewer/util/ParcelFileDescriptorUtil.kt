package com.horizontal.pdfviewer.util

import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object ParcelFileDescriptorUtil {
    @Throws(IOException::class)
    fun pipeFrom(inputStream: InputStream): ParcelFileDescriptor {
        val fileDescriptor: ParcelFileDescriptor = try {
            val file = createTemporaryFile()
            val fileOutputStream = FileOutputStream(file)
            inputStream.copyTo(fileOutputStream)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } finally {
            inputStream.close()
        }
        return fileDescriptor
    }

    @Throws(IOException::class)
    private fun createTemporaryFile(): File {
        return File.createTempFile("temp", null)
    }
}
