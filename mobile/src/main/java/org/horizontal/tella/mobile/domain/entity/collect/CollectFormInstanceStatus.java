package org.horizontal.tella.mobile.domain.entity.collect;


public enum CollectFormInstanceStatus {
    UNKNOWN,
    DRAFT,
    FINALIZED,
    SUBMITTED,
    SUBMISSION_ERROR,
    DELETED,
    SUBMISSION_PENDING, // no connection on sending, or offline mode - form saved
    SUBMISSION_PARTIAL_PARTS; // some req body parts (files) are note sent

    public boolean isFinal() {
        return !(this == UNKNOWN || this == DRAFT);
    }
}
