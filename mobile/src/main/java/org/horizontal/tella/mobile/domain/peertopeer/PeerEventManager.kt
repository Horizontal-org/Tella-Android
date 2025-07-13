package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest

/**
 * Created by wafa on 3/6/2025.
 */
object PeerEventManager {

    // MutableSharedFlow to emit registration events, replay 1 to send last event to new collectors
    val registrationEvents = MutableSharedFlow<Boolean>(replay = 1)
    private val _registrationRequests = MutableSharedFlow<Pair<String, PeerRegisterPayload>>(replay = 1)
    val registrationRequests = _registrationRequests.asSharedFlow()

    private val registrationDecisionMap = mutableMapOf<String, CompletableDeferred<Boolean>>()

    // Call this when registration succeeds
    suspend fun emitRegistrationSuccess() {
        registrationEvents.emit(true)
    }

    // we can add emitRegistrationFailure or other event types if needed
    private val _prepareUploadEvents = MutableSharedFlow<PrepareUploadRequest>(replay = 1)
    val prepareUploadEvents = _prepareUploadEvents.asSharedFlow()

    private val decisionMap = mutableMapOf<String, CompletableDeferred<Boolean>>()

    suspend fun emitPrepareUploadRequest(request: PrepareUploadRequest): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        decisionMap[request.sessionId] = deferred
        _prepareUploadEvents.emit(request)
        return deferred.await() // Wait for UI decision
    }

    fun resolveUserDecision(sessionId: String, accepted: Boolean) {
        decisionMap.remove(sessionId)?.complete(accepted)
    }

    suspend fun emitIncomingRegistrationRequest(registrationId: String, payload: PeerRegisterPayload): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        registrationDecisionMap[registrationId] = deferred
        _registrationRequests.emit(registrationId to payload)
        return deferred.await()
    }

    fun confirmRegistration(registrationId: String, accepted: Boolean) {
        registrationDecisionMap.remove(registrationId)?.complete(accepted)
    }
}
