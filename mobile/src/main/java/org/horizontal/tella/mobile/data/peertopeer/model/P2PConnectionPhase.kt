package org.horizontal.tella.mobile.data.peertopeer.model

enum class P2PConnectionPhase {
    IDLE,
    /** Step 1 complete — sender pinned receiver certificate hash. */
    RECEIVER_PINNED,
    /** Step 2 complete — both certificate hashes pinned; mTLS established. */
    MTLS_ESTABLISHED,
    REGISTERED,
}

enum class P2PVerificationStep {
    /** Confirm recipient / receiver certificate hash (protocol step 1). */
    RECIPIENT_HASH,
    /** Confirm sender certificate hash (protocol step 2). */
    SENDER_HASH,
}
