package rs.readahead.washington.mobile.domain.entity.uwazi

import com.google.gson.JsonObject
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus


data class UwaziEntityInstance(
    var id: Long = -1,
    var updated: Long = 0,
    var collectTemplate: CollectTemplate? = null,
    var metadata: JsonObject? = null,
    var status : UwaziEntityStatus = UwaziEntityStatus.UNKNOWN,
    var widgetMediaFiles: List<FormMediaFile> = emptyList(),
    val formPartStatus : FormMediaFileStatus = FormMediaFileStatus.UNKNOWN,
    var template: String = "",
    var title: String = "",
    val type: String = "entity")