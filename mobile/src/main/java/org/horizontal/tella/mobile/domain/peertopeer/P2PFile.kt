package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.serialization.Serializable

@Serializable
data class P2PFile(
    val id: String,
    val fileName: String,
    val size: Long,
    val fileType: String,
    val sha256: String,
    @Serializable(with = ByteArrayFlexibleSerializer::class)
    val thumbnail: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as P2PFile

        if (id != other.id) return false
        if (fileName != other.fileName) return false
        if (size != other.size) return false
        if (fileType != other.fileType) return false
        if (sha256 != other.sha256) return false
        if (thumbnail != null) {
            if (other.thumbnail == null) return false
            if (!thumbnail.contentEquals(other.thumbnail)) return false
        } else if (other.thumbnail != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + sha256.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }
}
