package rs.readahead.washington.mobile.views.fragment.uwazi.adapters

data class ViewEntityTemplateItem (
    val id: Long,
    val serverId: Long,
    val templateName: String,
    val serverName: String? = "",
    val isFavorite : Boolean = false,
    val onFavoriteClicked: () -> Unit,
    val onMoreClicked: () -> Unit,
    val onOpenEntityClicked : () -> Unit
) {
    fun onFavoriteClicked() = onFavoriteClicked.invoke()
    fun onMoreClicked() = onMoreClicked.invoke()
    fun onOpenEntityClicked() = onOpenEntityClicked.invoke()
}
