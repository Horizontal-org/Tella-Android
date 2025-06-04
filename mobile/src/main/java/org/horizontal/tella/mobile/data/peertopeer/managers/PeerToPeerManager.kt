package org.horizontal.tella.mobile.data.peertopeer.managers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PeerToPeerManager {
    private val _clientConnected = MutableSharedFlow<String>(replay = 0)
    val clientConnected = _clientConnected.asSharedFlow()

    suspend fun notifyClientConnected(hash : String) {
        _clientConnected.emit(hash)
    }
}