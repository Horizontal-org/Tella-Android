package rs.readahead.washington.mobile.views.dialog.dropbox

import android.os.Bundle
import com.dropbox.core.android.Auth
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.dialog.dropbox.connected.DropBoxConnectedServerFragment
import rs.readahead.washington.mobile.views.dialog.dropbox.utils.DropboxAppConfig
import rs.readahead.washington.mobile.views.dialog.dropbox.utils.DropboxCredentialUtil
import rs.readahead.washington.mobile.views.dialog.dropbox.utils.DropboxOAuthUtil

class DropBoxConnectFlowActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_dropbox)
        loginToDropbox()
    }

    // Function to initiate Dropbox login
    private fun loginToDropbox() {
        val dropboxCredentialUtil = DropboxCredentialUtil(this)
        val dropboxAppConfig = DropboxAppConfig()
        val dropBoxUtil = DropboxOAuthUtil(dropboxCredentialUtil, dropboxAppConfig)
        dropBoxUtil.startDropboxAuthorizationOAuth2(this)
    }

    // Handle the result after returning from Dropbox login flow
    override fun onResume() {
        super.onResume()
        val accessToken = Auth.getOAuth2Token()

        if (accessToken != null) {
            addFragment(DropBoxConnectedServerFragment.newInstance(DropBoxServer(), false), R.id.container)
        }
    }

}