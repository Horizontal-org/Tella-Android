package org.horizontal.tella.mobile.data.peertopeer.remote

sealed class RegisterPeerResult {
    data class Success(val sessionId: String) : RegisterPeerResult()
    data object InvalidFormat : RegisterPeerResult()      // 400
    data object InvalidPin : RegisterPeerResult()         // 401
    data object Conflict : RegisterPeerResult()           // 409
    data object TooManyRequests : RegisterPeerResult()    // 429
    data object ServerError : RegisterPeerResult()        // 500
    data object RejectedByReceiver : RegisterPeerResult() // 403
    data class Failure(val exception: Throwable) : RegisterPeerResult()
}
