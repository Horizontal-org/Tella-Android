package org.horizontal.tella.mobile.domain.entity.uwazi

import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus


data class UwaziEntityInstance(
    var id: Long = -1,
    var updated: Long = 0,
    var collectTemplate: UwaziTemplate? = null,
    var relationShipEntities: List<Value> = emptyList(),
    var metadata: Map<String, List<Any>> = mutableMapOf(),
    var status : EntityStatus = EntityStatus.UNKNOWN,
    var widgetMediaFiles: List<FormMediaFile> = emptyList(),
    var formPartStatus : FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var template: String = "",
    var title: String = "",
    var type: String = "entity") {
    var clonedId: Long = 0
}