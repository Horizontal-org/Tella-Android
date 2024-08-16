package rs.readahead.washington.mobile.views.dialog.nextcloud

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils.showBottomMessage
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.normalizeScheme
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.normalizeUrlSuffix
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles
import rs.readahead.washington.mobile.util.operations.DisplayUtils
import rs.readahead.washington.mobile.util.operations.GetServerInfoOperation
import rs.readahead.washington.mobile.util.operations.GetServerInfoOperation.ServerInfo
import rs.readahead.washington.mobile.util.operations.LoginUrlInfo
import rs.readahead.washington.mobile.util.operations.OperationsService
import rs.readahead.washington.mobile.util.operations.OperationsService.OperationsServiceBinder
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER
import rs.readahead.washington.mobile.views.dialog.nextcloud.SslUntrustedCertDialog.OnSslUntrustedCertListener
import rs.readahead.washington.mobile.views.dialog.nextcloud.step2.AuthenticatorAsyncTask.OnAuthenticatorTaskListener
import java.net.URLDecoder

@AndroidEntryPoint
class NextCloudLoginFlowActivity : BaseLockActivity(), OnSslUntrustedCertListener,
    OnRemoteOperationListener, OnAuthenticatorTaskListener {
    private val TAG: String = NextCloudLoginFlowActivity::class.java.simpleName
    private var mServerInfo = ServerInfo()
    private var mOperationsServiceBinder: OperationsServiceBinder? = null
    private var mWaitingForOpId = Long.MAX_VALUE
    private val PROTOCOL_SUFFIX: String = "://"
    private val LOGIN_URL_DATA_KEY_VALUE_SEPARATOR: String = ":"
    private val HTTPS_PROTOCOL: String = "https://"
    private val HTTP_PROTOCOL: String = "http://"

    private val mHandler = Handler()

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

            //  if (accountSetupBinding != null) {
            //     mServerStatusText = getResources().getString(R.string.auth_testing_connection);
            //     mServerStatusIcon = R.drawable.progress_small;
            //     showServerStatus();
            // }

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


    fun parseLoginDataUrl(prefix: String, dataString: String): LoginUrlInfo {
        require(dataString.length >= prefix.length) { "Invalid login URL detected" }

        val data = dataString.substring(prefix.length)
        val values = data.split("&")

        require(values.size in 1..3) { "Illegal number of login URL elements detected: ${values.size}" }

        val loginUrlInfo = LoginUrlInfo()

        for (value in values) {
            when {
                value.startsWith("user$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR") -> {
                    loginUrlInfo.username =
                        URLDecoder.decode(value.substring("user$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR".length))
                }

                value.startsWith("password$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR") -> {
                    loginUrlInfo.password =
                        URLDecoder.decode(value.substring("password$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR".length))
                }

                value.startsWith("server$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR") -> {
                    loginUrlInfo.serverAddress =
                        URLDecoder.decode(value.substring("server$LOGIN_URL_DATA_KEY_VALUE_SEPARATOR".length))
                }
            }
        }

        return loginUrlInfo
    }

    private fun doOnResumeAndBound() {
        mOperationsServiceBinder!!.addOperationListener(this, mHandler)
        if (mWaitingForOpId <= Int.MAX_VALUE) {
            mOperationsServiceBinder!!.dispatchResultIfFinished(mWaitingForOpId.toInt(), this)
        }
    }

    override fun onRemoteOperationFinish(
        operation: RemoteOperation<*>?,
        result: RemoteOperationResult<*>?
    ) {
        if (operation is GetServerInfoOperation) {
            if (operation.hashCode().toLong() == mWaitingForOpId) {
                //onGetServerInfoFinish(result)
            } // else nothing ; only the last check operation is considered;


            // multiple can be started if the user amends a URL quickly
        } else if (operation is GetUserInfoRemoteOperation) {
            // onGetUserNameFinish(result)
        }
    }

    override fun onAuthenticatorTaskCallback(result: RemoteOperationResult<UserInfo?>?) {

        Toast.makeText(this, "Test " + result?.isSuccess, Toast.LENGTH_LONG).show()

        //  Toast.makeText(this, result?.message, Toast.LENGTH_LONG).show()
    }


}