package rs.readahead.washington.mobile.views.dialog.dropbox

import android.os.Bundle
import androidx.activity.viewModels
import com.dropbox.core.android.Auth
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.dialog.dropbox.connected.DropBoxConnectedServerFragment
import rs.readahead.washington.mobile.views.dialog.dropbox.utils.DropboxOAuthUtil
import rs.readahead.washington.mobile.views.fragment.dropbox.data.RefreshDropBoxServer
import rs.readahead.washington.mobile.views.fragment.dropbox.send.REFRESH_SERVER_INTENT
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.SharedLiveData.refreshTokenServer
import javax.inject.Inject

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
        initObservers()
        maybeChangeTemporaryTimeout {
            loginToDropbox()
        }
    }

    private fun initObservers() {
        viewModel.refreshServerSuccess.observe(this)
        {
            refreshTokenServer.postValue(it)
            finish()
        }
    }

    // Function to initiate Dropbox login
    private fun loginToDropbox() {
        dropBoxUtil.startDropboxAuthorizationOAuth2(this)
    }

    // Handle the result after returning from Dropbox login flow
    override fun onResume() {
        super.onResume()
        val accessToken = Auth.getOAuth2Token()
        if (accessToken != null) {
            if (refreshDropBoxServer.isFromDropBoxSendView) {
                refreshDropBoxServer.server.token = accessToken
                viewModel.refreshDropBoxServer(refreshDropBoxServer.server)
            } else {
                val server = DropBoxServer(token = accessToken)
                addFragment(DropBoxConnectedServerFragment.newInstance(server), R.id.container)
            }
        }
    }

}