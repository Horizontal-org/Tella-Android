package org.horizontal.tella.mobile.domain.entity.feedback

data class FeedbackPostResult(
    val id: Int,
    val text: String,
    val platform: String,
    val updatedAt: String,
    val createdAt: String,
    val deletedAt: String
)

