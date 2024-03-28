package rs.readahead.washington.mobile.domain.entity.background_activity

data class BackgroundActivityModel(
    val id: String,
    val name: String,
    val type: BackgroundActivityType = BackgroundActivityType.FILE,
    val mimeType: String,
    val thumb: ByteArray? = null,
    val status: BackgroundActivityStatus = BackgroundActivityStatus.IN_PROGRESS
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackgroundActivityModel) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (mimeType != other.mimeType) return false
        if (!thumb.contentEquals(other.thumb)) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (thumb?.contentHashCode() ?: 0)
        result = 31 * result + status.hashCode()
        return result
    }
}