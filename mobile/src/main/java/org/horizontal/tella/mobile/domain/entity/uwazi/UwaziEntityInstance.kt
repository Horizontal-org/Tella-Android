package org.horizontal.tella.mobile.domain.entity.uwazi

import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.IEntityInstance
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus


data class UwaziEntityInstance(
    override var id: Long = -1,
    override var updated: Long = 0,
    var collectTemplate: UwaziTemplate? = null,
    var relationShipEntities: List<Value> = emptyList(),
    var metadata: Map<String, List<Any>> = mutableMapOf(),
    override var status : EntityStatus = EntityStatus.UNKNOWN,
    override var widgetMediaFiles: List<FormMediaFile> = emptyList(),
    var formPartStatus : FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var template: String = "",
    override var title: String = "",
    var type: String = "entity") : IEntityInstance {
    var clonedId: Long = 0

    override val serverId: Long
        get() = collectTemplate?.serverId ?: -1L
}