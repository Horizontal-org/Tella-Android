package rs.readahead.washington.mobile.domain.entity.reports

import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import java.io.Serializable

data class ReportFormInstance(
    var id: Long = -1,
    var serverId : Long = -1,
    var updated: Long = 0,
    var metadata: Map<String, List<Any>> = mutableMapOf(),
    var status: EntityStatus = EntityStatus.UNKNOWN,
    var widgetMediaFiles: List<FormMediaFile> = emptyList(),
    var formPartStatus: FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var title: String = "",
    var description: String = ""
) : Serializable
