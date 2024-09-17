package rs.readahead.washington.mobile.domain.entity.googledrive

import rs.readahead.washington.mobile.domain.entity.Server

class GoogleDriveServer @JvmOverloads constructor(
    id: Long = 0,
    var folderName: String = "",
    var folderId: String = "",
    val googleClientId: String =
        "166289458819-e5nt7d2lahv55ld0j527o07kovqdbip2.apps.googleusercontent.com"
) : Server()