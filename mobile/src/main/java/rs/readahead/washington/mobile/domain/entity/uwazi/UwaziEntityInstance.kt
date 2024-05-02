package rs.readahead.washington.mobile.domain.entity.uwazi

import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus


data class UwaziEntityInstance(
    var id: Long = -1,
    var updated: Long = 0,
    var collectTemplate: CollectTemplate? = null,
    var relationShipEntities: List<Value> = emptyList(),
    var metadata: Map<String, List<Any>> = mutableMapOf(),
    var status : EntityStatus = EntityStatus.UNKNOWN,
    var widgetMediaFiles: List<FormMediaFile> = emptyList(),
    var relationShipEntitiesSelected: List<String> = emptyList(),
    var formPartStatus : FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var template: String = "",
    var title: String = "",
    var type: String = "entity") {
    var clonedId: Long = 0
}