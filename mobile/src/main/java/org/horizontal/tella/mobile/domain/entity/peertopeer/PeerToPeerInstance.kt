package org.horizontal.tella.mobile.domain.entity.peertopeer

import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus

class PeerToPeerInstance(
    var id: Long = -1,
    var updated: Long = 0,
    var metadata: Map<String, List<Any>> = mutableMapOf(),
    var status: EntityStatus = EntityStatus.UNKNOWN,
    var widgetMediaFiles: List<FormMediaFile> = mutableListOf(),
    var formPartStatus: FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var title: String = "",
    var description: String = "",
    var current: Long = 0
)