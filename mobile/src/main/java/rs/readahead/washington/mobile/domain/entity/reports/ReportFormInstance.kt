package rs.readahead.washington.mobile.domain.entity.reports

import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import java.io.Serializable

data class ReportFormInstance(
    var id: Long = -1,
    var serverId: Long = -1,
    var reportApiId: String = "",
    var updated: Long = 0,
    var metadata: Map<String, List<Any>> = mutableMapOf(),
    var status: EntityStatus = EntityStatus.UNKNOWN,
    var widgetMediaFiles: List<FormMediaFile> = emptyList(),
    var formPartStatus: FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var title: String = "",
    var description: String = "",
    var current: Int = 0
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportFormInstance) return false


        if (title != other.title) return false
        if (description != other.description) return false
        if (widgetMediaFiles != other.widgetMediaFiles) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + widgetMediaFiles.hashCode()
        return result
    }
}
