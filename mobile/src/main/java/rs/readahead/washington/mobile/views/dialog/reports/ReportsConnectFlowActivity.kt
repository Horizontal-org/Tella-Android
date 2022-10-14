package rs.readahead.washington.mobile.views.dialog.reports

import android.os.Bundle
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.reports.ReportsConnectFlowActivity.*
import rs.readahead.washington.mobile.views.dialog.reports.edit.EditTellaServerFragment
import rs.readahead.washington.mobile.views.dialog.reports.step1.EnterUploadServerFragment

@AndroidEntryPoint
class ReportsConnectFlowActivity : BaseLockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uwazi_connect_flow)
        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            addFragment(EnterUploadServerFragment(), R.id.container)
        } else {
            intent.getStringExtra(OBJECT_KEY)?.let {
                val server = Gson().fromJson(it, TellaReportServer::class.java)
                addFragment(
                    EditTellaServerFragment.newInstance(server),
                    R.id.container
                )
            }
        }
    }
}