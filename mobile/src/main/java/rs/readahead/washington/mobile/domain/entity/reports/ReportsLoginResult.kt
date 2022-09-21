package rs.readahead.washington.mobile.domain.entity.reports

data class ReportsLoginResult(
    val accessToken: String,
    val user: User?
)