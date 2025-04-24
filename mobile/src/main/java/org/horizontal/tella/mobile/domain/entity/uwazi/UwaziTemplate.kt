package org.horizontal.tella.mobile.domain.entity.uwazi

data class UwaziTemplate(
    var id: Long = 0,
    var serverId: Long,
    var serverName: String? = "",
    var username: String? = "",
    var entityRow: UwaziRow,
    var isDownloaded: Boolean = false,
    var isFavorite: Boolean = false,
    var isUpdated: Boolean = false
)