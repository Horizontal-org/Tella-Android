package org.horizontal.tella.mobile.domain.entity.resources

import org.horizontal.tella.mobile.data.entity.reports.ProjectSlugResourceResponse

data class ListResourceResult(
    var errors: List<Throwable> = emptyList(),
    var slugs: List<ProjectSlugResourceResponse> = emptyList()
)