package org.horizontal.tella.mobile.data.peertopeer.remote

sealed class PeerUploadOutcome {
    data object Success : PeerUploadOutcome()
    data object Failed : PeerUploadOutcome()
    data object TooManyRequests : PeerUploadOutcome()
    data object PayloadTooLarge : PeerUploadOutcome()
}
