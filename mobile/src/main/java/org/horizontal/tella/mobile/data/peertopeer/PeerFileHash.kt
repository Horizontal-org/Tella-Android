package org.horizontal.tella.mobile.data.peertopeer

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object PeerFileHash {

    fun sha256Hex(inputStream: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            md.update(buffer, 0, read)
        }
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }

    fun sha256Hex(file: File): String =
        FileInputStream(file).use { sha256Hex(it) }
}
