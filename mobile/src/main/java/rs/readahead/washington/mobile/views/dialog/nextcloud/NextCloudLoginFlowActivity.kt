package rs.readahead.washington.mobile.views.dialog.nextcloud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import dagger.hilt.android.AndroidEntryPoint
import org.checkerframework.checker.units.qual.Length
import org.hzontal.shared_ui.utils.DialogUtils.showBottomMessage
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.normalizeScheme
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.normalizeUrlSuffix
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles
import rs.readahead.washington.mobile.util.operations.DisplayUtils
import rs.readahead.washington.mobile.util.operations.GetServerInfoOperation
import rs.readahead.washington.mobile.util.operations.GetServerInfoOperation.ServerInfo
import rs.readahead.washington.mobile.util.operations.OperationsService
import rs.readahead.washington.mobile.util.operations.OperationsService.OperationsServiceBinder
import rs.readahead.washington.mobile.views.activity.testnc.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.nextcloud.SslUntrustedCertDialog.OnSslUntrustedCertListener

@AndroidEntryPoint
class NextCloudLoginFlowActivity : BaseLockActivity(), OnSslUntrustedCertListener,
    OnRemoteOperationListener, INextCloudAuthFlow {
    private val TAG: String = NextCloudLoginFlowActivity::class.java.simpleName
    private var mServerInfo = ServerInfo()
    private var mOperationsServiceBinder: OperationsServiceBinder? = null
    private var mWaitingForOpId = Long.MAX_VALUE

    private val handler = Handler()
    private var ownCloudClient: OwnCloudClient? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nextcloud_connect_flow)

        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.nextcloud_settings)
        } else {
            //TODO EDIT SCREEN
        }
    }

    override fun onSavedCertificate(uri: String) {
        checkOcServer(uri)
    }

    override fun onFailedSavingCertificate() {
        showBottomMessage(this, getString(R.string.ssl_validator_not_saved), false)
    }

    private fun checkOcServer(uriServer: String) {
        var uri = uriServer
        mServerInfo = ServerInfo()

        if (uri.isNotEmpty()) {
            // if (accountSetupBinding != null) {
            uri = stripIndexPhpOrAppsFiles(uri)

            // accountSetupBinding.hostUrlInput.setText(uri);
            // }
            try {
                uri = normalizeScheme(uri)
            } catch (ex: IllegalArgumentException) {
                // Let the Nextcloud library check the error of the malformed URI
                Log_OC.e(TAG, "Invalid URL", ex)
            }

            // Handle internationalized domain names
            try {
                uri = DisplayUtils.convertIdn(uri, true)
            } catch (ex: IllegalArgumentException) {
                // Let the Nextcloud library check the error of the malformed URI
                Log_OC.e(
                    TAG,
                    "Error converting internationalized domain name $uri", ex
                )
            }

            // TODO maybe do this via async task
            val getServerInfoIntent = Intent()
            getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO)
            getServerInfoIntent.putExtra(
                OperationsService.EXTRA_SERVER_URL,
                normalizeUrlSuffix(uri)
            )

            if (mOperationsServiceBinder != null) {
                mWaitingForOpId = mOperationsServiceBinder!!.queueNewOperation(getServerInfoIntent)
            } else {
                Log_OC.e(
                    TAG,
                    "Server check tried with OperationService unbound!"
                )
            }
        }
    }


    override fun onRemoteOperationFinish(
        operation: RemoteOperation<*>?,
        result: RemoteOperationResult<*>?
    ) {
        if (result?.isSuccess == false) {
            Toast.makeText(this, R.string.todo_operation_finished_in_fail, Toast.LENGTH_SHORT)
                .show()
        } else if (operation is ReadFolderRemoteOperation) {
            onSuccessfulRefresh(operation as ReadFolderRemoteOperation?, result)
        }
    }

    private fun onSuccessfulRefresh(
        operation: ReadFolderRemoteOperation?,
        result: RemoteOperationResult<*>?
    ) {
        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
    }

    private fun onFailedFulRefresh(
        operation: ReadFolderRemoteOperation?,
        result: RemoteOperationResult<*>
    ) {

    }

    override fun onStartRefreshLogin(serverUrl: String, userName: String, password: String) {
        val serverUri = Uri.parse(getString(R.string.server_base_url))
        ownCloudClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, this, true)
        ownCloudClient?.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
            getString(R.string.username),
            getString(R.string.password)
        )
        ownCloudClient?.userId = getString(R.string.username)
        startRefresh()
    }

    private fun startRefresh() {
        val refreshOperation = ReadFolderRemoteOperation(FileUtils.PATH_SEPARATOR)
        refreshOperation.execute(ownCloudClient, this, handler)
    }

}