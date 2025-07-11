package org.horizontal.tella.mobile.data.peertopeer.model

import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.domain.peertopeer.P2PFile

data class ProgressFile(
    var file: P2PFile,
    var vaultFile : VaultFile? = null,
    var status: P2PFileStatus = P2PFileStatus.QUEUE,
    var transmissionId: String? = null,
    var path: String? = null,
    var bytesTransferred: Int = 0
)
