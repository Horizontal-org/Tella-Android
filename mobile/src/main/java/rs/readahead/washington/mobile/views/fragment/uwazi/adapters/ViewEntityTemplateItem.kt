package rs.readahead.washington.mobile.views.fragment.uwazi.adapters

data class ViewEntityTemplateItem (
    val id: Long,
    val serverId: Long,
    val templateName: String,
    val serverName: String? = "",
    val onFavoriteClicked: () -> Unit,
    val onDownloadClicked: () -> Unit,
) {
    fun onFavoriteClicked() = onFavoriteClicked.invoke()
    fun onDownloadClicked() = onDownloadClicked.invoke()
}
