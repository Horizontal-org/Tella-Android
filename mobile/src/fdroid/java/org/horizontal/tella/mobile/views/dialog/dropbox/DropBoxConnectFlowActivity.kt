package org.horizontal.tella.mobile.views.dialog.dropbox

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.views.base_ui.BaseActivity
import org.horizontal.tella.mobile.views.dialog.dropbox.utils.DropboxOAuthUtil
import org.horizontal.tella.mobile.views.fragment.dropbox.data.RefreshDropBoxServer
import org.horizontal.tella.mobile.views.fragment.dropbox.send.REFRESH_SERVER_INTENT
import javax.inject.Inject

/**
 * Stub implementation of DropBoxConnectFlowActivity for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but shows an error
 * message and finishes since Dropbox is not available in F-Droid builds.
 */
@AndroidEntryPoint
class DropBoxConnectFlowActivity : BaseActivity() {

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var dropBoxUtil: DropboxOAuthUtil

    private val viewModel by viewModels<DropBoxConnectFlowViewModel>()

    private var refreshDropBoxServer = RefreshDropBoxServer(false, DropBoxServer())

    private fun initView() {
        intent.getStringExtra(REFRESH_SERVER_INTENT)?.let {
            refreshDropBoxServer = gson.fromJson(it, RefreshDropBoxServer::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_dropbox)
        initView()
        Toast.makeText(
            this,
            "Dropbox is not available in F-Droid builds",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Stub implementation - no Dropbox OAuth handling
    }
}




