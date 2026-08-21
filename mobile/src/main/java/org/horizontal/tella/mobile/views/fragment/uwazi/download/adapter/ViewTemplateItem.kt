package org.horizontal.tella.mobile.views.fragment.uwazi.download.adapter

data class ViewTemplateItem(
    val id: String,
    val serverId: Long,
    val isDownloaded: Boolean,
    val isUpdated: Boolean,
    val templateName: String,
    val translatedTemplateName: String,
    val serverName: String? = "",
    val onMoreClicked: () -> Unit,
    val onDownloadClicked: () -> Unit,
) {
    fun onMoreClicked() = onMoreClicked.invoke()
    fun onDownloadClicked() = onDownloadClicked.invoke()
}