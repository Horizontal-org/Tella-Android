package rs.readahead.washington.mobile.data.entity.reports.mapper

import rs.readahead.washington.mobile.data.entity.reports.ReportPostResponse
import rs.readahead.washington.mobile.domain.entity.reports.ReportPostResult

fun ReportPostResponse.mapToDomainModel() = ReportPostResult(
    id = id ?: "",
    title = title ?: "",
    description = description ?: "",
    createdAt = createdAt ?: "createdAt"
)