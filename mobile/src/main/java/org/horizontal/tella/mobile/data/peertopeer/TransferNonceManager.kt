package org.horizontal.tella.mobile.data.peertopeer

import java.util.concurrent.ConcurrentHashMap

/**
 * Single-use nonce tracking for P2P prepare-upload and upload (protocol §5.2).
 * Matches iOS [NonceManager]: null/empty string rejected; no trimming (whitespace is a distinct nonce).
 */
class TransferNonceManager {

    private val seen = ConcurrentHashMap.newKeySet<String>()

    enum class AddResult {
        Success,
        Empty,
        Reused
    }

    fun tryAdd(nonce: String?): AddResult {
        if (nonce == null || nonce.isEmpty()) return AddResult.Empty
        if (!seen.add(nonce)) return AddResult.Reused
        return AddResult.Success
    }

    fun clear() {
        seen.clear()
    }
}
