package rs.readahead.washington.mobile.views.fragment.uwazi.adapters

import rs.readahead.washington.mobile.domain.entity.EntityStatus

data class ViewEntityInstanceItem (
    val id: Long,
    val serverId: Long?,
    val instanceName: String,
    val serverName: String = "",
    val translatedTemplateName : String,
    val updated : Long,
    val status : EntityStatus,
    val onMoreClicked: () -> Unit,
    val onOpenClicked: () -> Unit,
) {
    fun onMoreClicked() = onMoreClicked.invoke()
    fun onOpenClicked() = onOpenClicked.invoke()
}
