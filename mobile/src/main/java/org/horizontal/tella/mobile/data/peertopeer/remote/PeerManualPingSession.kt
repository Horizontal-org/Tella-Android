package org.horizontal.tella.mobile.data.peertopeer.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

/**
 * An in-flight manual `/api/v2/ping`
 *
 * The receiver (server) certificate hash resolves from the TLS handshake as soon as the connection
 * is established — before the server-held HTTP response — so the sender can navigate to the
 * receiver-hash verification screen immediately. [awaitSenderShowHash] resolves later, from the held
 * HTTP body, once the recipient confirms the receiver hash on their device.
 */
class PeerManualPingSession(
    private val receiverHash: CompletableDeferred<String>,
    private val senderShowHash: CompletableDeferred<Boolean>,
    private val job: Job,
) {
    /** Receiver leaf cert hash from the TLS handshake (resolves quickly). */
    suspend fun awaitReceiverHash(): String = receiverHash.await()

    /** `senderShowHash` from the held HTTP body (resolves after the recipient confirms). */
    suspend fun awaitSenderShowHash(): Boolean = senderShowHash.await()

    fun cancel() {
        job.cancel()
    }
}
