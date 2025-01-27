package org.horizontal.tella.mobile.views.fragment.reports.mappers

import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem

fun ReportInstance.toViewEntityInstanceItem(
    onMoreClicked: () -> Unit,
    onOpenClicked: () -> Unit
) = ViewEntityTemplateItem(
    id = id,
    title = title,
    serverId = serverId,
    updated = updated,
    status = status,
    serverName = "",
    description = "",
    onMoreClicked = onMoreClicked,
    onOpenEntityClicked = onOpenClicked
)