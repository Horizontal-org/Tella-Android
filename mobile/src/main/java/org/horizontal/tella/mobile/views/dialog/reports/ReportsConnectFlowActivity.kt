package org.horizontal.tella.mobile.views.dialog.reports

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.reports.edit.EditTellaServerFragment

@AndroidEntryPoint
class ReportsConnectFlowActivity : BaseLockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uwazi_connect_flow)
        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.reports_settings)
        } else {
            intent.getStringExtra(OBJECT_KEY)?.let { reportServer ->
                val server = Gson().fromJson(reportServer, TellaReportServer::class.java)
                addFragment(
                    EditTellaServerFragment.newInstance(server, true),
                    R.id.container
                )
            }
        }
    }
}