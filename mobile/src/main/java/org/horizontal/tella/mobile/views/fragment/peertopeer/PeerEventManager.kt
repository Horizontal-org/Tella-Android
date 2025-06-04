package org.horizontal.tella.mobile.views.fragment.peertopeer

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.flow.MutableSharedFlow
import org.horizontal.tella.mobile.data.peertopeer.PrepareUploadRequest

/**
 * Created by wafa on 3/6/2025.
 */
object PeerEventManager {

    // MutableSharedFlow to emit registration events, replay 1 to send last event to new collectors
    val registrationEvents = MutableSharedFlow<Boolean>(replay = 1)

    // Call this when registration succeeds
    suspend fun emitRegistrationSuccess() {
        registrationEvents.emit(true)
    }

    // we can add emitRegistrationFailure or other event types if needed


    val prepareUploadEvents =
        MutableSharedFlow<Pair<PrepareUploadRequest, (Boolean) -> Unit>>(replay = 0)

    suspend fun emitPrepareUploadRequest(
        request: PrepareUploadRequest,
        callback: (Boolean) -> Unit
    ) {
        prepareUploadEvents.emit(request to callback)
    }
}
