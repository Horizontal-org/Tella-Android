package rs.readahead.washington.mobile.views.dialog.nextcloud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils.showBottomMessage
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.normalizeScheme
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.normalizeUrlSuffix
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles
import rs.readahead.washington.mobile.util.operations.DisplayUtils
import rs.readahead.washington.mobile.util.operations.GetServerInfoOperation.ServerInfo
import rs.readahead.washington.mobile.util.operations.OperationsService
import rs.readahead.washington.mobile.util.operations.OperationsService.OperationsServiceBinder
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
    private var nextCloudServer = NextCloudServer()
    private val viewModel by viewModels<NextCloudLoginFlowViewModel>()
    private var folderPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nextcloud_connect_flow)

        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.nextcloud_settings)
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
                    TAG, "Error converting internationalized domain name $uri", ex
                )
            }

            // TODO maybe do this via async task
            val getServerInfoIntent = Intent()
            getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO)
            getServerInfoIntent.putExtra(
                OperationsService.EXTRA_SERVER_URL, normalizeUrlSuffix(uri)
            )

            if (mOperationsServiceBinder != null) {
                mWaitingForOpId = mOperationsServiceBinder!!.queueNewOperation(getServerInfoIntent)
            } else {
                Log_OC.e(
                    TAG, "Server check tried with OperationService unbound!"
                )
            }
        }
    }


    override fun onRemoteOperationFinish(
        operation: RemoteOperation<*>?, result: RemoteOperationResult<*>?
    ) {
        viewModel.progress.postValue(false)
        if (result?.isSuccess == false) {
            if (operation is ReadFolderRemoteOperation) {
                viewModel.errorUserNamePassword.postValue(true)
            }
        } else if (operation is ReadFolderRemoteOperation) {
            onSuccessfulRefresh()
        } else if (operation is CreateFolderRemoteOperation) {
            onSuccessfulRemoteServerOperation()
        }
    }

    private fun onSuccessfulRemoteServerOperation() {
        viewModel.progress.postValue(false)

        ownCloudClient.let {
            nextCloudServer.folderName = folderPath
            viewModel.successLoginToServer.postValue(nextCloudServer)
        }
    }

    private fun onSuccessfulRefresh(
    ) {
        viewModel.progress.postValue(false)

        ownCloudClient.let {
            nextCloudServer.userId = ownCloudClient!!.userId
            nextCloudServer.username = ownCloudClient!!.credentials.username
            nextCloudServer.password = ownCloudClient!!.credentials.authToken
            viewModel.successLoginToServer.postValue(nextCloudServer)
        }

    }

    override fun onStartRefreshLogin(serverUrl: String, userName: String, password: String) {
        val serverUri = Uri.parse(serverUrl)
        ownCloudClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, this, true)
        ownCloudClient?.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
            userName, password
        )
        ownCloudClient?.userId = userName

        nextCloudServer.apply {
            name = userName
            this.password = password
        }
        startRefresh()
    }

    override fun onStartCreateRemoteFolder(folderName: String) {
        createRemoteFolder(folderName)
    }

    private fun startRefresh() {
        val refreshOperation = ReadFolderRemoteOperation(FileUtils.PATH_SEPARATOR)
        refreshOperation.execute(ownCloudClient, this, handler)
    }

    private fun createRemoteFolder(folderPath: String) {
        this.folderPath = folderPath
        val createFolderOperation = CreateFolderRemoteOperation(folderPath, true)
        createFolderOperation.execute(ownCloudClient, this, handler)
    }

}