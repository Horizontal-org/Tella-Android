package rs.readahead.washington.mobile.views.fragment.reports.adapter

import rs.readahead.washington.mobile.domain.entity.EntityStatus

data class ViewEntityTemplateItem(
    val id: Long,
    val serverId: Long,
    val title: String,
    val description: String,
    val serverName: String? = "",
    val updated: Long = 0,
    val isFavorite: Boolean = false,
    val status: EntityStatus,
    val onMoreClicked: () -> Unit,
    val onOpenEntityClicked: () -> Unit
) {
    fun onMoreClicked() = onMoreClicked.invoke()
    fun onOpenEntityClicked() = onOpenEntityClicked.invoke()
}