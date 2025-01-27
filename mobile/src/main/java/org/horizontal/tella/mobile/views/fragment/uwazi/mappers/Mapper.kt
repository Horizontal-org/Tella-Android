package org.horizontal.tella.mobile.views.fragment.uwazi.mappers

import org.horizontal.tella.mobile.domain.entity.uwazi.CollectTemplate
import org.horizontal.tella.mobile.domain.entity.uwazi.Language
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.views.adapters.uwazi.ViewLanguageItem
import org.horizontal.tella.mobile.views.fragment.uwazi.adapters.ViewEntityInstanceItem
import org.horizontal.tella.mobile.views.fragment.uwazi.adapters.ViewEntityTemplateItem
import org.horizontal.tella.mobile.views.fragment.uwazi.download.adapter.ViewTemplateItem

fun CollectTemplate.toViewTemplateItem(onMoreClicked: () -> Unit, onDownloadClicked: () -> Unit) =
    ViewTemplateItem(
        id = entityRow._id,
        isDownloaded = isDownloaded,
        serverId = serverId,
        isUpdated = isUpdated,
        templateName = entityRow.name,
        translatedTemplateName = entityRow.translatedName,
        serverName = serverName,
        onDownloadClicked = onDownloadClicked,
        onMoreClicked = onMoreClicked
    )

fun CollectTemplate.toViewEntityTemplateItem(
    onFavoriteClicked: () -> Unit,
    onMoreClicked: () -> Unit,
    onOpenEntityClicked: () -> Unit
) = ViewEntityTemplateItem(
    id = id,
    serverId = serverId,
    templateName = entityRow.name,
    translatedTemplateName =  entityRow.translatedName,
    serverName = serverName,
    isFavorite = isFavorite,
    onMoreClicked = onMoreClicked,
    onFavoriteClicked = onFavoriteClicked,
    onOpenEntityClicked = onOpenEntityClicked
)

fun Language.toViewLanguageItem(onLanguageClicked: () -> Unit) = ViewLanguageItem(
    languageSmallText = label,
    languageBigText = label,
    key = key,
    default = default,
    onLanguageClicked = onLanguageClicked)

fun UwaziEntityInstance.toViewEntityInstanceItem(
    onMoreClicked: () -> Unit,
    onOpenClicked: () -> Unit
) = ViewEntityInstanceItem(
    id = id,
    instanceName = title,
    serverId = collectTemplate?.serverId,
    updated = updated,
    status = status,
    translatedTemplateName = collectTemplate?.entityRow?.translatedName ?: "",
    serverName = collectTemplate?.serverName ?: "",
    onMoreClicked = onMoreClicked,
    onOpenClicked = onOpenClicked
)
