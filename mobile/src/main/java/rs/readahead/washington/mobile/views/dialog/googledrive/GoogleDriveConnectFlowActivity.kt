package rs.readahead.washington.mobile.views.dialog.googledrive

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.reports.edit.EditTellaServerFragment


class GoogleDriveConnectFlowActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_drive)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            navController.navigate(R.id.google_drive)
        } else {
            intent.getStringExtra(OBJECT_KEY)?.let { googleDriveServer ->

            }
        }
    }

}



