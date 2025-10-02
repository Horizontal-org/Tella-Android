package org.horizontal.tella.mobile.views.dialog.nextcloud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils.showBottomMessage
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.util.operations.AuthenticatorUrlUtils
import org.horizontal.tella.mobile.util.operations.DisplayUtils
import org.horizontal.tella.mobile.util.operations.GetServerInfoOperation
import org.horizontal.tella.mobile.util.operations.OperationsService
import org.horizontal.tella.mobile.views.base_ui.BaseLockActivity
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.nextcloud.sslalert.SslUntrustedCertDialog.OnSslUntrustedCertListener

@AndroidEntryPoint
class NextCloudLoginFlowActivity : BaseLockActivity(),
    OnSslUntrustedCertListener,
    OnRemoteOperationListener,
    INextCloudAuthFlow {

    private val viewModel by viewModels<NextCloudLoginFlowViewModel>()
    private val handler = Handler(Looper.getMainLooper())

    private var ownCloudClient: OwnCloudClient? = null
    private var mOperationsServiceBinder: OperationsService.OperationsServiceBinder? = null
    private var mWaitingForOpId: Long = Long.MAX_VALUE
    private var mServerInfo = GetServerInfoOperation.ServerInfo()

    private val nextCloudServer = NextCloudServer()
    private var folderPath: String = ""
    private var password: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nextcloud_connect_flow)

        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            navigateToNextcloudSettings()
        }
    }

    private fun navigateToNextcloudSettings() {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)
            ?.navController
            ?.navigate(R.id.nextcloud_settings)
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

            operation is ReadFolderRemoteOperation -> {
                viewModel.errorUserNamePassword.postValue(true)
            }

            operation is CreateFolderRemoteOperation -> {
                viewModel.errorFolderCreation.postValue(result?.message)
            }
        }
    }

    private fun onSuccessfulCreateFolderOperation() {
        viewModel.progress.postValue(false)
        nextCloudServer.folderName = folderPath
        viewModel.successFolderCreation.postValue(nextCloudServer)
    }

    /**
     * After a successful "/", fetch current user info.
     */
    private fun onSuccessfulRefresh() {
        val client = ownCloudClient ?: return
        viewModel.progress.postValue(true)

        GetUserInfoRemoteOperation().execute(
            client,
            { _: RemoteOperation<*>, result: RemoteOperationResult<*> ->
                viewModel.progress.postValue(false)
                if (result.isSuccess) {
                    val userInfo = result.resultData as? UserInfo
                    nextCloudServer.apply {
                        userId = userInfo?.id.orEmpty()
                        username = userInfo?.displayName?.takeIf { it.isNotBlank() }
                            ?: userInfo?.id.orEmpty()
                        password = this@NextCloudLoginFlowActivity.password
                    }
                    viewModel.successLoginToServer.postValue(nextCloudServer)
                } else {
                    // Auth worked, but user info failed → surface a sensible error
                    viewModel.errorUserNamePassword.postValue(true)
                }
            },
            handler
        )
    }

    override fun onStartRefreshLogin(serverUrl: String, userName: String, password: String) {
        ownCloudClient = OwnCloudClientFactory.createOwnCloudClient(
            Uri.parse(serverUrl), this, true
        ).apply {
            credentials = OwnCloudCredentialsFactory.newBasicCredentials(userName, password)
        }

        this.password = password
        nextCloudServer.apply {
            name = userName
            this.password = password
            url = serverUrl
        }

        startRefresh()
    }

    override fun onStartCreateRemoteFolder(folderName: String) = createRemoteFolder(folderName)

    private fun startRefresh() {
        ReadFolderRemoteOperation(FileUtils.PATH_SEPARATOR)
            .execute(ownCloudClient, this, handler)
    }

    private fun createRemoteFolder(targetFolderPath: String) {
        // remember for success callback
        this.folderPath = targetFolderPath

        val parentFolderPath =
            targetFolderPath.substringBeforeLast("/", missingDelimiterValue = "/")

        // 1) Ensure parent is listable
        ReadFolderRemoteOperation(parentFolderPath).execute(
            ownCloudClient,
            { _, parentResult ->
                if (!parentResult.isSuccess) {
                    // Parent not listable → try to create the full path
                    CreateFolderRemoteOperation(targetFolderPath, true).execute(
                        ownCloudClient,
                        { _, createResult ->
                            if (createResult.isSuccess) {
                                onSuccessfulCreateFolderOperation()
                            } else {
                                viewModel.progress.postValue(false)
                                viewModel.errorFolderCreation.postValue(createResult?.message)
                            }
                        },
                        handler
                    )
                } else {
                    // 2) Check child existence
                    ReadFolderRemoteOperation(targetFolderPath).execute(
                        ownCloudClient,
                        { _, childResult ->
                            if (childResult.isSuccess) {
                                viewModel.progress.postValue(false)
                                viewModel.errorFolderNameExist.postValue(
                                    getString(R.string.folder_exist_error)
                                )
                            } else {
                                // 3) Create
                                CreateFolderRemoteOperation(targetFolderPath, true).execute(
                                    ownCloudClient,
                                    { _, createResult ->
                                        if (createResult.isSuccess) {
                                            onSuccessfulCreateFolderOperation()
                                        } else {
                                            viewModel.progress.postValue(false)
                                            viewModel.errorFolderCreation.postValue(createResult?.message)
                                        }
                                    },
                                    handler
                                )
                            }
                        },
                        handler
                    )
                }
            },
            handler
        )
    }

    companion object {
        private val TAG = NextCloudLoginFlowActivity::class.java.simpleName
    }
}
