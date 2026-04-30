package org.horizontal.tella.mobile.domain.peertopeer

data class IncomingRegistration(
    val registrationId: String,
    val payload: PeerRegisterPayload
)