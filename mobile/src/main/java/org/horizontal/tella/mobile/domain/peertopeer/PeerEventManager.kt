package org.horizontal.tella.mobile.domain.peertopeer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.horizontal.tella.mobile.data.peertopeer.model.P2PSharedState
import org.horizontal.tella.mobile.data.peertopeer.remote.PrepareUploadRequest
import org.horizontal.tella.mobile.views.fragment.peertopeer.viewmodel.state.UploadProgressState

/**
 * Created by wafa on 3/6/2025.
 */
object PeerEventManager {

    val registrationEvents = MutableSharedFlow<Boolean>(replay = 1)
    private val _registrationRequests =
        MutableSharedFlow<Pair<String, PeerRegisterPayload>>(replay = 1)
    val registrationRequests = _registrationRequests.asSharedFlow()
    private val registrationDecisionMap = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val _prepareUploadEvents = MutableSharedFlow<PrepareUploadRequest>(replay = 1)
    val prepareUploadEvents = _prepareUploadEvents.asSharedFlow()
    private val decisionMap = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val _uploadProgressStateFlow = MutableSharedFlow<UploadProgressState>(replay = 1)
    val uploadProgressStateFlow = _uploadProgressStateFlow.asSharedFlow()

    suspend fun onUploadProgressState(state: UploadProgressState) {
        _uploadProgressStateFlow.emit(state)
    }

    suspend fun emitRegistrationSuccess() {
        registrationEvents.emit(true)
    }

    suspend fun emitPrepareUploadRequest(request: PrepareUploadRequest): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        decisionMap[request.sessionId] = deferred
        _prepareUploadEvents.emit(request)
        return deferred.await()
    }

    fun resolveUserDecision(sessionId: String, accepted: Boolean) {
        decisionMap.remove(sessionId)?.complete(accepted)
    }

    suspend fun emitIncomingRegistrationRequest(
        registrationId: String,
        payload: PeerRegisterPayload
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        registrationDecisionMap[registrationId] = deferred
        _registrationRequests.emit(registrationId to payload)
        return deferred.await()
    }

    fun confirmRegistration(registrationId: String, accepted: Boolean) {
        registrationDecisionMap.remove(registrationId)?.complete(accepted)
    }

}
