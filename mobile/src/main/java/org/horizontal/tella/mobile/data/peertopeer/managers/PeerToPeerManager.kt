package org.horizontal.tella.mobile.data.peertopeer.managers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PeerToPeerManager {
    // Keep the latest ping/cert-hash event for late collectors (manual recipient flow can subscribe after ping).
    private val _clientConnected = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val clientConnected = _clientConnected.asSharedFlow()

    /** Receiver should show recipient-hash verification after manual ping (protocol step 1). */
    private val _recipientHashVerification = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val recipientHashVerification = _recipientHashVerification.asSharedFlow()

    suspend fun notifyClientConnected(hash: String) {
        _clientConnected.emit(hash)
    }

    suspend fun notifyRecipientHashVerification() {
        _recipientHashVerification.emit(Unit)
    }

    fun clearClientConnected() {
        _clientConnected.resetReplayCache()
    }

    fun clearRecipientHashVerification() {
        _recipientHashVerification.resetReplayCache()
    }
}