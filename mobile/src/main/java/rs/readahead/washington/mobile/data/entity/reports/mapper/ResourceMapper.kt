package rs.readahead.washington.mobile.data.entity.reports.mapper

import rs.readahead.washington.mobile.data.entity.reports.ProjectSlugResourceResponse
import rs.readahead.washington.mobile.domain.entity.resources.Resource

fun ProjectSlugResourceResponse.mapToDomainModel() = ProjectSlugResourceResponse(
    id = id,
    name = name,
    slug = slug,
    url = url,
    resources = resources.map { it.mapToDomainModel() },
    createdAt = createdAt
)

fun Resource.mapToDomainModel() = Resource(
    resourceId = -1,
    serverId = -1,
    id = id,
    title = title,
    fileName = fileName,
    size = size,
    createdAt = createdAt,
    savedAt = 0,
    project = null,
    fileId = null
)