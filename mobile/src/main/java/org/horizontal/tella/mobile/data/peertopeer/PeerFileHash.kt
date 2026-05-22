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

    /** Lowercase hex SHA-256 and total bytes read (must match what we send over the wire). */
    fun sha256HexAndLength(inputStream: InputStream): Pair<String, Long> {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            md.update(buffer, 0, read)
            total += read
        }
        val hex = md.digest().joinToString("") { b -> "%02x".format(b) }
        return hex to total
    }

    fun sha256Hex(file: File): String =
        FileInputStream(file).use { sha256Hex(it) }
}
