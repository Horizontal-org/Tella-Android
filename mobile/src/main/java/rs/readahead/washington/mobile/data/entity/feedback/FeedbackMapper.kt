package rs.readahead.washington.mobile.data.entity.feedback

import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult

fun FeedbackPostResponse.mapToDomainModel() = FeedbackPostResult(
    id = id ,
    platform = platform ?: "",
    text = text ?: "",
    createdAt = createdAt ?: "createdAt",
    updatedAt = updatedAt ?: "updatedAt",
    deletedAt = deletedAt ?: "deletedAt"
)
