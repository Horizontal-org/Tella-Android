package org.horizontal.tella.mobile.domain.peertopeer

import com.hzontal.tella_vault.VaultFile

/**
 * Limits for Nearby Sharing (prepare + upload), aligned with Tella-Desktop defaults
 * (`maxFileSizeBytes` / `maxFileCount` in app config) `NearbySharingTransferConfig.standard`.
 */
object NearbySharingTransferConfig {

    object Standard {
        /** Default 3 GB — matches desktop `config.defaultMaxFileSize`. */
        const val maxFileSizeBytes: Long = 3_000_000_000L

        /** Matches desktop `defaultMaxFileCount`. */
        const val maxFileCount: Int = 1000
    }

    /** Pre-check using vault metadata before we stream for plaintext hash (conservative on size). */
    fun isSelectionWithinVaultLimits(files: List<VaultFile>): Boolean {
        if (files.size > Standard.maxFileCount) return false
        if (files.any { it.size > Standard.maxFileSizeBytes }) return false
        return true
    }
}
