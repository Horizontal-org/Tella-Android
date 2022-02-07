package rs.readahead.washington.mobile.views.fragment.uwazi.adapters

data class ViewEntityInstanceItem (
    val id: Long,
    val serverId: Long?,
    val instanceName: String,
    val serverName: String? = "",
    val onMoreClicked: () -> Unit,
    val onOpenClicked: () -> Unit,
) {
    fun onMoreClicked() = onMoreClicked.invoke()
    fun onOpenClicked() = onOpenClicked.invoke()
}
