package org.horizontal.tella.mobile.data.entity.reports

import org.horizontal.tella.mobile.domain.entity.resources.Resource

data class ProjectSlugResourceResponse(val id: String, val name: String, val slug: String, val url: String, val resources: List<Resource>, val createdAt: String)