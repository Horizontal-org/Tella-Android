package rs.readahead.washington.mobile.views.dialog.nextcloud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.owncloud.android.lib.common.*
import com.owncloud.android.lib.common.operations.*
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.*
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils.showBottomMessage
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.util.operations.*
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.nextcloud.sslalert.SslUntrustedCertDialog.OnSslUntrustedCertListener

@AndroidEntryPoint
class NextCloudLoginFlowActivity : BaseLockActivity(), OnSslUntrustedCertListener,
    OnRemoteOperationListener, INextCloudAuthFlow {

    private val viewModel by viewModels<NextCloudLoginFlowViewModel>()
    private val handler = Handler()
    private var ownCloudClient: OwnCloudClient? = null
    private var mOperationsServiceBinder: OperationsService.OperationsServiceBinder? = null
    private var mWaitingForOpId = Long.MAX_VALUE
    private var mServerInfo = GetServerInfoOperation.ServerInfo()
    private var nextCloudServer = NextCloudServer()
    private var folderPath = ""
    private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nextcloud_connect_flow)

        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            navigateToNextcloudSettings()
        }
    }

    private fun navigateToNextcloudSettings() {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)?.navController?.navigate(
            R.id.nextcloud_settings
        )
    }

    override fun onSavedCertificate(uri: String) = checkOcServer(uri)

    override fun onFailedSavingCertificate() =
        showBottomMessage(this, getString(R.string.ssl_validator_not_saved), false)

    private fun checkOcServer(uriServer: String) {
        var uri = uriServer.trim()
        mServerInfo = GetServerInfoOperation.ServerInfo()

        if (uri.isNotEmpty()) {
            uri = uri
                .run { AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles(this) }
                .runCatching { AuthenticatorUrlUtils.normalizeScheme(this) }
                .getOrElse {
                    Log_OC.e(TAG, "Invalid URL", it)
                    return
                }

            uri = DisplayUtils.convertIdn(uri, true).also {
                uri = AuthenticatorUrlUtils.normalizeUrlSuffix(it)
            }

            if (mOperationsServiceBinder != null) {
                val getServerInfoIntent = Intent(OperationsService.ACTION_GET_SERVER_INFO).apply {
                    putExtra(OperationsService.EXTRA_SERVER_URL, uri)
                }
                mWaitingForOpId = mOperationsServiceBinder!!.queueNewOperation(getServerInfoIntent)
            } else {
                Log_OC.e(TAG, "Server check attempted with an unbound OperationService")
            }
        }
    }

    override fun onRemoteOperationFinish(
        operation: RemoteOperation<*>?,
        result: RemoteOperationResult<*>?
    ) {
        viewModel.progress.postValue(false)
        when {
            result?.isSuccess == true -> when (operation) {
                is ReadFolderRemoteOperation -> onSuccessfulRefresh()
                is CreateFolderRemoteOperation -> onSuccessfulCreateFolderOperation()
            }

            operation is ReadFolderRemoteOperation -> viewModel.errorUserNamePassword.postValue(true)
            operation is CreateFolderRemoteOperation -> viewModel.errorFolderCreation.postValue(
                result?.message
            )
        }
    }

    private fun onSuccessfulCreateFolderOperation() {
        viewModel.progress.postValue(false)
        ownCloudClient?.let {
            nextCloudServer.folderName = folderPath
            viewModel.successFolderCreation.postValue(nextCloudServer)
        }
    }

    private fun onSuccessfulRefresh() {
        viewModel.progress.postValue(false)
        ownCloudClient?.let {
            nextCloudServer.apply {
                userId = it.userId
                username = it.credentials.username
                password = this@NextCloudLoginFlowActivity.password
            }
            viewModel.successLoginToServer.postValue(nextCloudServer)
        }
    }

    override fun onStartRefreshLogin(serverUrl: String, userName: String, password: String) {
        ownCloudClient =
            OwnCloudClientFactory.createOwnCloudClient(Uri.parse(serverUrl), this, true).apply {
                credentials = OwnCloudCredentialsFactory.newBasicCredentials(userName, password)
                userId = userName
            }
        this.password = password
        nextCloudServer.apply { name = userName; this.password = password }
        startRefresh()
    }

    override fun onStartCreateRemoteFolder(folderName: String) = createRemoteFolder(folderName)

    private fun startRefresh() {
        ReadFolderRemoteOperation(FileUtils.PATH_SEPARATOR).execute(ownCloudClient, this, handler)
    }

    private fun createRemoteFolder(folderPath: String) {
        this.folderPath = folderPath
        CreateFolderRemoteOperation(folderPath, true).execute(ownCloudClient, this, handler)
    }

    companion object {
        private val TAG = NextCloudLoginFlowActivity::class.java.simpleName
    }
}
