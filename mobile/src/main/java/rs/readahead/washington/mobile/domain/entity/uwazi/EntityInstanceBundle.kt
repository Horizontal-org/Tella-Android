package rs.readahead.washington.mobile.domain.entity.uwazi

data class EntityInstanceBundle (
    var instance: UwaziEntityInstance = UwaziEntityInstance(),
    var fileIds: Array<String> = emptyArray()
)