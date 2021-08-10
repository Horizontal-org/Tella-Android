package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data

import rs.readahead.washington.mobile.domain.entity.MediaFile

data class VaultFile (var id // generated uuid
                      : Int,
                      var type: MediaFile.Type? = null,
                      var hash: String? = null,
                      var path: String? = null,
                      var mimeType: String? = null,
                      var name: String = "",
                      var size: Long = 0,
                      var created: Long = 0,
                      var duration: Long = 0,
                      var anonymous : Boolean = false,
                      var metadata: Metadata? = null,
                      var thumb: ByteArray?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VaultFile

        if (id != other.id) return false
        if (type != other.type) return false
        if (hash != other.hash) return false
        if (path != other.path) return false
        if (mimeType != other.mimeType) return false
        if (name != other.name) return false
        if (size != other.size) return false
        if (created != other.created) return false
        if (duration != other.duration) return false
        if (anonymous != other.anonymous) return false
        if (metadata != other.metadata) return false
        if (!other.thumb?.let { thumb?.contentEquals(it) }!!) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (hash?.hashCode() ?: 0)
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + (name.hashCode() ?: 0)
        result = 31 * result + size.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + anonymous.hashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + thumb?.contentHashCode()!!
        return result
    }
}


