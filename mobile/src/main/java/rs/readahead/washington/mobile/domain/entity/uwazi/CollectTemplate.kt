package rs.readahead.washington.mobile.domain.entity.uwazi

import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow

data class CollectTemplate (
    var id: Long = 0,
    var serverId: Long,
    var serverName: String? = null,
    var username: String? = null,
    var entityRow: UwaziEntityRow,
    var isDownloaded : Boolean = false,
    var isFavorite : Boolean = false,
    var isUpdated : Boolean = false)