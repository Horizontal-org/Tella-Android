package rs.readahead.washington.mobile.domain.entity.googledrive

import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType

class GoogleDriveServer @JvmOverloads constructor(
    id: Long = 0,
    var folderName: String = "",
    var folderId: String = "",
    val googleClientId: String =
        "299748721134-2gfc8r94auvg8rvj92f2hqvptrrbc6a9.apps.googleusercontent.com"
) : Server() {
    init {
        serverType = ServerType.GOOGLE_DRIVE
        setId(id)
        name = "Google Drive"
    }
}