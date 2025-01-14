package org.horizontal.tella.mobile.views.dialog.uwazi

import android.os.Bundle
import com.google.gson.Gson
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.uwazi.step1.EnterServerFragment

class UwaziConnectFlowActivity : BaseLockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uwazi_connect_flow)
        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            addFragment(EnterServerFragment(), R.id.container)
        } else {
            intent.getStringExtra(OBJECT_KEY)?.let {
                val server = Gson().fromJson(it, UWaziUploadServer::class.java)
                addFragment(EnterServerFragment.newInstance(server, true), R.id.container)
            }

        }
    }

}