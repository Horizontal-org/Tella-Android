package rs.readahead.washington.mobile.domain.entity.uwazi

import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow

data class CollectTemplate (
    var id: Long = 0,
    var serverId: Long,
    var serverName: String? = "",
    var username: String? = "",
    var entityRow: UwaziEntityRow,
    var isDownloaded : Boolean = false,
    var isFavorite : Boolean = false,
    var isUpdated : Boolean = false)