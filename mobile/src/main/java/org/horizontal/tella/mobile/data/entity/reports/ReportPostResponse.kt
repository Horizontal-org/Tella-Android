package org.horizontal.tella.mobile.data.entity.reports

import org.horizontal.tella.mobile.domain.entity.reports.Author

data class ReportPostResponse (
    val id: String?, val title: String?, val description: String?,
    val createdAt: String?, val author: Author?
        )