package org.horizontal.tella.mobile.data.entity.feedback

data class FeedbackPostResponse(
    val id: Int, val platform: String?, val updatedAt: String?,
    val createdAt: String?, val deletedAt: String?, val text: String?
)