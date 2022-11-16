package rs.readahead.washington.mobile.views.fragment.reports.mappers

import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem

fun ReportFormInstance.toViewEntityInstanceItem(
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