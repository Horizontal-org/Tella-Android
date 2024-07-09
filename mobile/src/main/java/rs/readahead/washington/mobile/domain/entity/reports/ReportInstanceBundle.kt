package rs.readahead.washington.mobile.domain.entity.reports


data class ReportInstanceBundle (
    var instance: ReportInstance = ReportInstance(),
    var fileIds: Array<String> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReportInstanceBundle

        if (instance != other.instance) return false
        if (!fileIds.contentEquals(other.fileIds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = instance.hashCode()
        result = 31 * result + fileIds.contentHashCode()
        return result
    }
}