package org.horizontal.tella.mobile.domain.entity.peertopeer

import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus

class PeerToPeerInstance(
    var id: Long = -1,
    var updated: Long = 0,
    var status: EntityStatus = EntityStatus.UNKNOWN,
    var widgetMediaFiles: List<FormMediaFile> = mutableListOf(),
    var formPartStatus: FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var title: String = "",
    var current: Long = 0,
    var sessionID: String = ""
)