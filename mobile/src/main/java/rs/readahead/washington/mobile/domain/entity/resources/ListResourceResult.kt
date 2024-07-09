package rs.readahead.washington.mobile.domain.entity.resources

import rs.readahead.washington.mobile.data.entity.reports.ProjectSlugResourceResponse

data class ListResourceResult(
    var errors: List<Throwable> = emptyList(),
    var slugs: List<ProjectSlugResourceResponse> = emptyList()
)