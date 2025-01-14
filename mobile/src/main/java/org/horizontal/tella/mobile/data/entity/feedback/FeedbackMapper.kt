package org.horizontal.tella.mobile.data.entity.feedback

import org.horizontal.tella.mobile.domain.entity.feedback.FeedbackPostResult

fun FeedbackPostResponse.mapToDomainModel() = FeedbackPostResult(
    id = id ,
    platform = platform ?: "",
    text = text ?: "",
    createdAt = createdAt ?: "createdAt",
    updatedAt = updatedAt ?: "updatedAt",
    deletedAt = deletedAt ?: "deletedAt"
)
