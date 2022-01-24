package rs.readahead.washington.mobile.views.fragment.uwazi.mappers

import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.uwazi.download.adapter.ViewTemplateItem

fun CollectTemplate.toViewTemplateItem(onMoreClicked: () -> Unit, onDownloadClicked: () -> Unit) =
    ViewTemplateItem(
        id = entityRow._id,
        isDownloaded = isDownloaded,
        serverId = serverId,
        isUpdated = isUpdated,
        templateName = entityRow.name,
        serverName = serverName,
        onDownloadClicked = onDownloadClicked,
        onMoreClicked = onMoreClicked
    )

fun CollectTemplate.toViewEntityTemplateItem(
    onFavoriteClicked: () -> Unit,
    onMoreClicked: () -> Unit
) = ViewEntityTemplateItem(
    id = id,
    serverId = serverId,
    templateName = entityRow.name,
    serverName = serverName,
    onMoreClicked = onMoreClicked,
    onFavoriteClicked = onFavoriteClicked

)