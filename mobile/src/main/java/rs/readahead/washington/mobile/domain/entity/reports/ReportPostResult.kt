package rs.readahead.washington.mobile.domain.entity.reports

data class ReportPostResult(
    val id: String, val title: String, val description: String,
    val createdAt: String, val deviceInfo: String?, val author: Author,
)