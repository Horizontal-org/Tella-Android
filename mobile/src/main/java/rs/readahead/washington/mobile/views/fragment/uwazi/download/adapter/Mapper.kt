package rs.readahead.washington.mobile.views.fragment.uwazi.download.adapter

import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate

fun CollectTemplate.toViewTemplateItem(onMoreClicked: () -> Unit, onDownloadClicked: () -> Unit) = ViewTemplateItem(
    id =  id,
    isDownloaded = isDownloaded,
    serverId = serverId,
    isUpdated = isUpdated,
    templateName = entityRow.name,
    serverName = serverName,
    onDownloadClicked = onDownloadClicked,
    onMoreClicked = onMoreClicked
)