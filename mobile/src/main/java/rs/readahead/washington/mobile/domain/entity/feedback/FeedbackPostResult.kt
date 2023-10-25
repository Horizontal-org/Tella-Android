package rs.readahead.washington.mobile.domain.entity.feedback

data class FeedbackPostResult(
    val id: String,
    val text: String,
    val platform: String,
    val updatedAt: String,
    val createdAt: String,
    val deletedAt: String
)

