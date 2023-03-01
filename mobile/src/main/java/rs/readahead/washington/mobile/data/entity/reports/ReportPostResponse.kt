package rs.readahead.washington.mobile.data.entity.reports

import rs.readahead.washington.mobile.domain.entity.reports.Author

data class ReportPostResponse (
    val id: String?, val title: String?, val description: String?,
    val createdAt: String?, val author: Author?
        )