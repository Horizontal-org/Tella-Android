package org.horizontal.tella.mobile.data.entity.reports.mapper

import org.horizontal.tella.mobile.data.entity.reports.ReportPostResponse
import org.horizontal.tella.mobile.domain.entity.reports.ReportPostResult

fun ReportPostResponse.mapToDomainModel() = ReportPostResult(
    id = id ?: "",
    title = title ?: "",
    description = description ?: "",
    createdAt = createdAt ?: "createdAt"
)