package org.horizontal.tella.mobile.domain.entity.feedback

enum class FeedbackStatus {
    UNKNOWN,
    DRAFT,
    SUBMISSION_IN_PROGRESS,
    SUBMISSION_ERROR,
    SUBMISSION_PENDING;

    open fun isFinal(): Boolean {
        return !(this == UNKNOWN || this == DRAFT)
    }
}
