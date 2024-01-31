package rs.readahead.washington.mobile.views.dialog.reports

import android.os.Bundle
import androidx.fragment.app.commit
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.reports.edit.EDIT_MODE_KEY
import rs.readahead.washington.mobile.views.dialog.reports.edit.EditTellaServerFragment
import rs.readahead.washington.mobile.views.dialog.reports.step1.EnterUploadServerFragment

@AndroidEntryPoint
class ReportsConnectFlowActivity : BaseLockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uwazi_connect_flow)
        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.reports_settings)
//            supportFragmentManager.commit {
//                add(
//                    R.id.container,
//                    EnterUploadServerFragment(),
//                    EnterUploadServerFragment::class.java.simpleName
//                )
//           }
        } else {
            intent.getStringExtra(OBJECT_KEY)?.let { reportServer ->
                val server = Gson().fromJson(reportServer, TellaReportServer::class.java)
                addFragment(
                    EditTellaServerFragment.newInstance(server,true),
                    R.id.container
                )
            }

        }
    }
}