package rs.readahead.washington.mobile.data.entity.reports.mapper

import rs.readahead.washington.mobile.data.entity.reports.ProjectSlugResourceResponse
import rs.readahead.washington.mobile.domain.entity.reports.ResourceTemplate

fun ProjectSlugResourceResponse.mapToDomainModel() = ProjectSlugResourceResponse(
    id = id,
    name = name,
    slug = slug,
    url = url,
    resources = resources.map { it.mapToDomainModel() } ?: emptyList(),
    createdAt = createdAt
)

fun ResourceTemplate.mapToDomainModel() = ResourceTemplate(
    id = id,
    title = title,
    fileName = fileName,
    size = size,
    createdAt = createdAt
)