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

    // Replays the last actual registration request to new collectors
    private val _registrationRequests = MutableSharedFlow<Pair<String, PeerRegisterPayload>>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val registrationRequests = _registrationRequests.asSharedFlow()

    // Optional: refactor this one later too
    private val _prepareUploadEvents = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val prepareUploadEvents = _prepareUploadEvents.asSharedFlow()

    private var pendingPrepareUploadRequest: PrepareUploadRequest? = null

    private val decisionMap = mutableMapOf<String, CompletableDeferred<Boolean>>()
    private val registrationDecisionMap = mutableMapOf<String, CompletableDeferred<Boolean>>()

    private val _uploadProgressStateFlow = MutableSharedFlow<UploadProgressState>(replay = 1)
    val uploadProgressStateFlow = _uploadProgressStateFlow.asSharedFlow()

    // Upload progress
    suspend fun onUploadProgressState(state: UploadProgressState) {
        _uploadProgressStateFlow.emit(state)
    }

    // Trigger one-time event for registration success
    suspend fun emitRegistrationSuccess() {
        registrationEvents.emit(true)
    }

    // Trigger upload preparation request, storing the data separately
    suspend fun emitPrepareUploadRequest(request: PrepareUploadRequest): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        decisionMap[request.sessionId] = deferred
        pendingPrepareUploadRequest = request
        _prepareUploadEvents.emit(Unit)
        return deferred.await()
    }

    fun getPendingPrepareUploadRequest(): PrepareUploadRequest? {
        return pendingPrepareUploadRequest.also {
            pendingPrepareUploadRequest = null // Clear once accessed
        }
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
