package rs.readahead.washington.mobile.domain.entity.uwazi

import com.google.gson.JsonObject


data class UwaziEntityInstance(
    var id: Long = -1,
    var updated: Long = 0,
    var collectTemplate: CollectTemplate? = null,
    var metadata: JsonObject? = null,
    var status : UwaziEntityStatus = UwaziEntityStatus.UNKNOWN
)