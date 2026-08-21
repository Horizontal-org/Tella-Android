package org.horizontal.tella.mobile.domain.entity

enum class EntityStatus {
    UNKNOWN,
    DRAFT,
    FINALIZED,
    SUBMITTED,
    SUBMISSION_ERROR,
    DELETED,
    PAUSED,
    SUBMISSION_PENDING, // no connection on sending, or offline mode - form saved
    SUBMISSION_PARTIAL_PARTS, // some req body parts (files) are note sent
    SCHEDULED,
    SUBMISSION_IN_PROGRESS;

    open fun isFinal(): Boolean {
        return !(this == UNKNOWN || this == DRAFT)
    }
}
