package rs.readahead.washington.mobile.domain.entity.googledrive

import android.content.Context
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.ServerType
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportsUtils.loadConfig

class GoogleDriveServer @JvmOverloads constructor(
    id: Long = 0,
    var folderName: String = "",
    var folderId: String = "",
    context: Context // Add context as a parameter to access the config
) : Server() {

    val googleClientId: String // Declare googleClientId as a variable

    init {
        // Load the configuration and get googleClientId
        val config = loadConfig(context)
        googleClientId = config.googleClientId // Assign the value from config

        serverType = ServerType.GOOGLE_DRIVE
        setId(id)
        name = "Google Drive"
    }
}