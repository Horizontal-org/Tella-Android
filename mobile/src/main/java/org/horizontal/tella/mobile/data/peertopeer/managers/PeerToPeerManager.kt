package org.horizontal.tella.mobile.data.peertopeer.managers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PeerToPeerManager {
    // Keep the latest ping/cert-hash event for late collectors (manual recipient flow can subscribe after iOS ping).
    private val _clientConnected = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val clientConnected = _clientConnected.asSharedFlow()

    suspend fun notifyClientConnected(hash : String) {
        _clientConnected.emit(hash)
    }

    fun clearClientConnected() {
        _clientConnected.resetReplayCache()
    }
}