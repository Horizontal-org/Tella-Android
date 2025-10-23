package org.horizontal.tella.mobile.views.dialog.nextcloud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.navigation.fragment.NavHostFragment
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.Credentials
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
    INextCloudAuthFlow {

    private val viewModel by viewModels<NextCloudLoginFlowViewModel>()
    private val handler = Handler(Looper.getMainLooper())

    // Switched to NextcloudClient only
    private var nextcloudClient: NextcloudClient? = null

    // Optional: if you still use your OperationsService elsewhere
    private var mOperationsServiceBinder: OperationsService.OperationsServiceBinder? = null
    private var mWaitingForOpId: Long = Long.MAX_VALUE
    private var mServerInfo = GetServerInfoOperation.ServerInfo()

    private val nextCloudServer = NextCloudServer()
    private var folderPath: String = ""

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

    /**
     * Optional server check using your OperationsService queue.
     * (Unchanged behavior; uses your existing operation if you still need it.)
     */
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

            // Optional: enforce HTTPS (recommended)
            val parsed = Uri.parse(uri)
            if (parsed.scheme.equals("http", true)) {
              //  showBottomMessage(this, getString(R.string.nextcloud_https_required), false)
                return
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

    /**
     * INextCloudAuthFlow — start login with NextcloudClient
     */
    override fun onStartRefreshLogin(serverUrl: String, userName: String, password: String) {
        // Build NextcloudClient with Basic auth
        val credentials = Credentials.basic(userName, password)
        val userId = userName // temporary; will be replaced by the real UID after GetUserInfo

        nextcloudClient = NextcloudClient(
            serverUrl.toUri(),
            userId,
            credentials,
            this
        )

        // Do not store the raw password in app state
        nextCloudServer.apply {
            name = userName
            url = serverUrl
        }

        // Fetch real user info (UID/display name) and finalize login
        fetchCurrentUser()
    }

    /**
     * INextCloudAuthFlow — create folder at target path
     */
    override fun onStartCreateRemoteFolder(folderName: String) = createRemoteFolder(folderName)

    /**
     * Fetch user info using NextcloudClient (synchronous op on background thread)
     */
    private fun fetchCurrentUser() {
        val client = nextcloudClient ?: return
        viewModel.progress.postValue(true)

        lifecycleScope.launch {
            val result: RemoteOperationResult<*> = withContext(Dispatchers.IO) {
                GetUserInfoRemoteOperation().execute(client) // sync call
            }

            viewModel.progress.postValue(false)

            if (result.isSuccess) {
                val info = result.resultData as UserInfo

                // If your NextcloudClient supports updating userId, do it here
                // (The constructor sets an initial userId, but we prefer the real UID returned)
                // Some versions expose 'userId' as a val; if so, you can recreate the client if needed.
                // For most flows, storing UID for future ops is sufficient:
                nextCloudServer.apply {
                    userId = info.id.toString()
                    username = info.displayName.takeIf { !it.isNullOrBlank() } ?: info.id
                }

                viewModel.successLoginToServer.postValue(nextCloudServer)
            } else {
                viewModel.errorUserNamePassword.postValue(true)
            }
        }
    }

    /**
     * Create remote folder with a single recursive MKCOL call.
     * No pre-reads required.
     */
    private fun createRemoteFolder(targetFolderPath: String) {
        val client = nextcloudClient ?: return
        folderPath = targetFolderPath
        viewModel.progress.postValue(true)

        lifecycleScope.launch {
            val result: RemoteOperationResult<*> = withContext(Dispatchers.IO) {
                CreateFolderRemoteOperation(targetFolderPath, /* createFullPath = */ true)
                    .execute(client) // sync call
            }

            viewModel.progress.postValue(false)
            when {
                result.isSuccess -> {
                    nextCloudServer.folderName = folderPath
                    viewModel.successFolderCreation.postValue(nextCloudServer)
                }
                // Nextcloud usually returns 405 if the folder already exists (MKCOL on existing)
                result.httpCode == 405 -> {
                    viewModel.errorFolderNameExist.postValue(getString(R.string.folder_exist_error))
                }
                else -> {
                    viewModel.errorFolderCreation.postValue(result.message)
                }
            }
        }
    }

    companion object {
        private val TAG = NextCloudLoginFlowActivity::class.java.simpleName
    }
}
