package org.horizontal.tella.mobile.data.peertopeer.model

import org.horizontal.tella.mobile.domain.peertopeer.P2PFile

data class ProgressFile(
    var file: P2PFile,
    var status: P2PFileStatus = P2PFileStatus.QUEUE,
    var transmissionId: String? = null,
    var path: String? = null,
    var bytesTransferred: Int = 0
)
