package rs.readahead.washington.mobile.data.entity.reports

import rs.readahead.washington.mobile.domain.entity.resources.Resource

data class ProjectSlugResourceResponse(val id: String, val name: String, val slug: String, val url: String, val resources: List<Resource>, val createdAt: String)