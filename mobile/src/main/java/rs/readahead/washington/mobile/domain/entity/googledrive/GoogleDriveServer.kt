package rs.readahead.washington.mobile.domain.entity.googledrive

import android.content.Context
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportsUtils.loadConfig

class GoogleDriveServer @JvmOverloads constructor(
    id: Long = 0, // id is optional and defaults to 0
    context: Context // context is required to load config.json
) : Server() {

    val googleClientId: String // Declare googleClientId as a variable
    var folderName: String = ""
    var folderId: String = ""
    var serverId: Long = id // Store id here

    init {
        // Load the configuration and get googleClientId
        val config = loadConfig(context)
        googleClientId = config.googleClientId // Assign the value from config
        serverType = ServerType.GOOGLE_DRIVE
        setId(id)
        name = "Google Drive"
    }
}