package org.horizontal.tella.mobile.domain.entity.reports

import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus
import java.io.Serializable

data class ReportInstance(
    var id: Long = -1,
    var serverId: Long = -1,
    //TODO REPLACEMENT
    var reportApiId: String = "",
    var updated: Long = 0,
    var metadata: Map<String, List<Any>> = mutableMapOf(),
    var status: EntityStatus = EntityStatus.UNKNOWN,
    var widgetMediaFiles: List<FormMediaFile> = mutableListOf(),
    var formPartStatus: FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var title: String = "",
    var description: String = "",
    var current: Long = 0
) : Serializable {


    companion object {

        @JvmStatic
        fun getAutoReportReportInstance(serverId: Long, title: String): ReportInstance {
            return ReportInstance(
                serverId = serverId,
                title = title,
                status = EntityStatus.SCHEDULED,
                current = 1)
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReportInstance) return false


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
