package org.horizontal.tella.mobile.domain.entity.googledrive

import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.ServerType

class GoogleDriveServer @JvmOverloads constructor(
    id: Long = 0,
    var folderName: String = "",
    var folderId: String = "",
    val googleClientId: String = ""
) : Server() {
    init {
        serverType = ServerType.GOOGLE_DRIVE
        setId(id)
        name = "Google drive"
    }
}