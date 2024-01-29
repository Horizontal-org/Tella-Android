package rs.readahead.washington.mobile.data.entity.reports

import rs.readahead.washington.mobile.domain.entity.reports.ResourceTemplate

data class ProjectSlugResourceResponse(val id: String, val name: String, val slug: String, val url: String, val resources: List<ResourceTemplate>, val createdAt: String)