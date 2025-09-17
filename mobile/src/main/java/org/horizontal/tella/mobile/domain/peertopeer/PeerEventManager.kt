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

    // Used to signal clearing of registration replayed value
    private val EMPTY_REGISTRATION_REQUEST = "" to PeerRegisterPayload.EMPTY

    val registrationEvents = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 1
    )

    val closeConnectionEvent = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 1
    )

    // Replays the last actual registration request to new collectors
    private val _registrationRequests = MutableSharedFlow<Pair<String, PeerRegisterPayload>>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val registrationRequests = _registrationRequests.asSharedFlow()


    private val _prepareUploadRequests =
        MutableSharedFlow<PrepareUploadRequest>(replay = 1, extraBufferCapacity = 1)
    val prepareUploadRequests = _prepareUploadRequests.asSharedFlow()


    private var pendingPrepareUploadRequest: PrepareUploadRequest? = null

    private val decisionMap = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val registrationDecisionMap = mutableMapOf<String, CompletableDeferred<Boolean>>()

    private val _uploadProgressStateFlow = MutableSharedFlow<UploadProgressState>(replay = 0)
    val uploadProgressStateFlow = _uploadProgressStateFlow.asSharedFlow()

    // Upload progress
    suspend fun onUploadProgressState(state: UploadProgressState) {
        _uploadProgressStateFlow.emit(state)
    }

    // Trigger one-time event for registration success
    suspend fun emitRegistrationSuccess() {
        registrationEvents.emit(true)
    }

    suspend fun emitCloseConnection() {
        closeConnectionEvent.emit(true)
    }

    suspend fun emitPrepareUploadRequest(request: PrepareUploadRequest): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        decisionMap[request.sessionId] = deferred
        _prepareUploadRequests.emit(request)  // emit actual payload; late subscribers will receive it
        return deferred.await()               // await user decision on recipient
    }


    fun resolveUserDecision(sessionId: String, accepted: Boolean) {
        decisionMap.remove(sessionId)?.complete(accepted)
    }

    // Registration request with real payload
    suspend fun emitIncomingRegistrationRequest(
        registrationId: String,
        payload: PeerRegisterPayload
    ): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        registrationDecisionMap[registrationId] = deferred
        _registrationRequests.emit(registrationId to payload)
        return deferred.await()
    }

    // Clears the replayed registration request by emitting a dummy one
    suspend fun clearRegistrationRequest() {
        _registrationRequests.emit(EMPTY_REGISTRATION_REQUEST)
    }

    fun confirmRegistration(registrationId: String, accepted: Boolean) {
        registrationDecisionMap.remove(registrationId)?.complete(accepted)
    }
}
