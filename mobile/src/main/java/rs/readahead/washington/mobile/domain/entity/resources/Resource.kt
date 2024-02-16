package rs.readahead.washington.mobile.domain.entity.resources


data class Resource(
    var resourceId: Long = -1,
    var serverId: Long = -1,
    var id: String? = "",
    var title: String? = "",
    var fileName: String = "",
    var size: Long,
    var createdAt: String? = "",
    var savedAt: Long? = 0,
    var project: String? = "",
    var fileId: String?
)
