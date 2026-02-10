package org.horizontal.tella.mobile.views.dialog.googledrive

import android.os.Bundle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.base_ui.BaseActivity
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.googledrive.setp0.OBJECT_KEY

class GoogleDriveConnectFlowActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_drive)

        val migrationServerJson = intent.getStringExtra(EXTRA_MIGRATION_SERVER)
        if (!migrationServerJson.isNullOrEmpty()) {
            val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            navHost?.navController?.let { nav ->
                val args = Bundle().apply {
                    putString(OBJECT_KEY, migrationServerJson)
                    putBoolean(IS_UPDATE_SERVER, true)
                }
                val options = NavOptions.Builder()
                    .setPopUpTo(R.id.googleDriveConnectFragment, true)
                    .build()
                nav.navigate(R.id.createFolderFragment, args, options)
            }
        }
    }

    companion object {
        const val EXTRA_MIGRATION_SERVER = "extra_migration_server"
        const val EXTRA_RESULT_SERVER_ID = "extra_result_server_id"
        const val EXTRA_RESULT_FOLDER_ID = "extra_result_folder_id"
        const val EXTRA_RESULT_FOLDER_NAME = "extra_result_folder_name"
    }
}



