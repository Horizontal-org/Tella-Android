package rs.readahead.washington.mobile.domain.entity.feedback

enum class FeedbackStatus {
    UNKNOWN,
    DRAFT,
    SUBMISSION_IN_PROGRESS,
    SUBMISSION_ERROR,
    SUBMISSION_PENDING;
    // no connection on sending, or offline mode - form saved

    open fun isFinal(): Boolean {
        return !(this == UNKNOWN || this == DRAFT)
    }
}
