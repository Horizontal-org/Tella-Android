package org.horizontal.tella.mobile.views.fragment.uwazi.mappers

import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.domain.entity.uwazi.Language
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.util.StringUtils
import org.horizontal.tella.mobile.views.adapters.uwazi.ViewLanguageItem
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import org.horizontal.tella.mobile.views.fragment.uwazi.adapters.ViewUwaziTemplateItem
import org.horizontal.tella.mobile.views.fragment.uwazi.download.adapter.ViewTemplateItem
import java.util.Locale

fun UwaziTemplate.toViewTemplateItem(onMoreClicked: () -> Unit, onDownloadClicked: () -> Unit) =
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

fun UwaziTemplate.toViewUwaziTemplateItem(
    onFavoriteClicked: () -> Unit,
    onMoreClicked: () -> Unit,
    onOpenEntityClicked: () -> Unit
) = ViewUwaziTemplateItem(
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

fun Language.toViewLanguageItem(onLanguageClicked: () -> Unit): ViewLanguageItem {
    val locale = Locale.forLanguageTag(key.replace('_', '-'))
    val uiLocale = Locale.getDefault()

    // Native name on top (e.g. Français, عربي), current Tella language name below (e.g. French, Arabic)
    val nativeName = locale.getDisplayName(locale).ifBlank { label }
    val translatedName = locale.getDisplayName(uiLocale).ifBlank { label }

    return ViewLanguageItem(
        languageBigText = capitalizeLanguageName(nativeName, locale),
        languageSmallText = capitalizeLanguageName(translatedName, uiLocale),
        key = key,
        default = default,
        onLanguageClicked = onLanguageClicked
    )
}

private fun capitalizeLanguageName(name: String, locale: Locale): String {
    if (name.isEmpty()) return name
    return StringUtils.capitalize(name, locale)
}

/**
 * Maps a Uwazi entity onto the row model the shared draft/outbox/submitted list renders. The
 * entity's template name takes the `description` slot, which the shared adapter shows as the
 * row's secondary line.
 */
fun UwaziEntityInstance.toViewEntityInstanceItem(
    onMoreClicked: () -> Unit,
    onOpenClicked: () -> Unit
) = ViewEntityTemplateItem(
    id = id,
    serverId = serverId,
    title = title,
    description = collectTemplate?.entityRow?.translatedName ?: "",
    serverName = collectTemplate?.serverName ?: "",
    updated = updated,
    status = status,
    onMoreClicked = onMoreClicked,
    onOpenEntityClicked = onOpenClicked
)
