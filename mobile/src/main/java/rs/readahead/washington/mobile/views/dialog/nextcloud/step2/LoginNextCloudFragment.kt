package rs.readahead.washington.mobile.views.dialog.nextcloud.step2

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.owncloud.android.lib.common.OwnCloudCredentials
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.utils.Log_OC
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentLoginScreenBinding
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.util.operations.AuthenticatorUrlUtils
import rs.readahead.washington.mobile.util.operations.DisplayUtils
import rs.readahead.washington.mobile.util.operations.GetServerInfoOperation
import rs.readahead.washington.mobile.util.operations.OperationsService
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.nextcloud.NextCloudLoginFlowViewModel
import timber.log.Timber

@AndroidEntryPoint
class LoginNextCloudFragment : BaseBindingFragment<FragmentLoginScreenBinding>(
    FragmentLoginScreenBinding::inflate
) {
    private val viewModel: NextCloudLoginFlowViewModel by viewModels()
    private lateinit var serverNextCloud: NextCloudServer
    private var validated = true
    private var mOperationsServiceConnection: ServiceConnection? = null
    private val mOperationsServiceBinder: OperationsService.OperationsServiceBinder? = null
    private var mAsyncTask: AuthenticatorAsyncTask? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
        initObservers()
    }

    fun initView() {
        arguments?.getString(OBJECT_KEY)?.let {
            serverNextCloud = Gson().fromJson(it, NextCloudServer::class.java)
        }
    }

    private fun initListeners() {
        binding.loginButton.setOnClickListener {
            if (!MyApplication.isConnectedToInternet(baseActivity)) {
                DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(R.string.settings_docu_error_no_internet),
                    true
                )
            } else {
                validate()
                if (validated) {

                    checkBasicAuthorization( binding.username.text.toString(), binding.password.text.toString())

                   // startService()
                  /* viewModel.checkUserCredentials(
                        serverNextCloud.url,
                        binding.username.text.toString(),
                        binding.password.text.toString(),
                    )*/
                }
            }
        }
        binding.backBtn.setOnClickListener { baseActivity.onBackPressed() }
    }


    private fun validate() {
        validated = true
        validateRequired(binding.username, binding.usernameLayout)
        validateRequired(binding.password, binding.passwordLayout)
    }

    private fun validateRequired(field: EditText?, layout: TextInputLayout?) {
        layout?.error = null
        if (TextUtils.isEmpty(field!!.text.toString())) {
            layout?.error = getString(R.string.settings_text_empty_field)
            validated = false
        }
    }

    @SuppressLint("TimberArgCount")
    private fun initObservers() {
        viewModel.userInfoResult.observe(viewLifecycleOwner) { userInfoResult ->
            if (userInfoResult.isSuccess) {
                // Operation was successful, access result data
                val userInfo = userInfoResult.resultData
                // Proceed with the successful result, e.g., navigate to the next screen or update UI
            } else {
                // Operation failed, handle the failure
                Timber.e("LoginNextCloudFragment", "Operation failed: ${userInfoResult.logMessage}")
                // Show an error message to the user or take appropriate action
                Toast.makeText(context, "Failed to validate server URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Tests the credentials entered by the user performing a check of existence on the root folder of the ownCloud
     * server.
     */
    private fun checkBasicAuthorization(webViewUsername: String?, webViewPassword: String?) {

        // validate credentials accessing the root folder
        val credentials = OwnCloudCredentialsFactory.newBasicCredentials(
            webViewUsername,
            webViewPassword
        )
        accessRootFolder(credentials)
    }

    private fun accessRootFolder(credentials: OwnCloudCredentials) {
        mAsyncTask = AuthenticatorAsyncTask(baseActivity)
        val params = arrayListOf<Any>()
        params.add(serverNextCloud.url)
        params.add(credentials)
        Log.d("AccessRootFolder", "Params size: ${params.size}")

        mAsyncTask?.execute(params)
    }

   private fun startService() {

        // bind to Operations Service
        mOperationsServiceConnection = OperationsServiceConnection()
        if (!baseActivity.bindService(
                Intent(baseActivity, OperationsService::class.java),
                mOperationsServiceConnection as OperationsServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        ) {
            Toast.makeText(
                baseActivity,
                "error_cant_bind_to_operations_service",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun checkOcServer(uri: String) {
        var serverUri = uri

        val serverInfo = GetServerInfoOperation.ServerInfo()

        if (uri.isNotEmpty()) {
            serverUri = AuthenticatorUrlUtils.stripIndexPhpOrAppsFiles(serverUri)

            try {
                serverUri = AuthenticatorUrlUtils.normalizeScheme(uri)
            } catch (ex: IllegalArgumentException) {
                // Let the Nextcloud library check the error of the malformed URI
                Log_OC.e(
                    "LoginNextCloudFragment",
                    "Invalid URL",
                    ex
                )
            }

            // Handle internationalized domain names
            try {
                serverUri = DisplayUtils.convertIdn(uri, true)
            } catch (ex: IllegalArgumentException) {
                // Let the Nextcloud library check the error of the malformed URI
                Log_OC.e(
                    "LoginNextCloudFragment",
                    "Error converting internationalized domain name $uri", ex
                )
            }

            // TODO maybe do this via async task
            val getServerInfoIntent = Intent()
            getServerInfoIntent.setAction(OperationsService.ACTION_GET_SERVER_INFO)
            getServerInfoIntent.putExtra(
                OperationsService.EXTRA_SERVER_URL,
                AuthenticatorUrlUtils.normalizeUrlSuffix(uri)
            )

            if (mOperationsServiceBinder != null) {
                val mWaitingForOpId =
                    mOperationsServiceBinder.queueNewOperation(getServerInfoIntent)
            } else {
                Log_OC.e(
                    "AuthenticatorActivity",
                    "Server check tried with OperationService unbound!"
                )
            }
        }
    }

    inner class OperationsServiceConnection : ServiceConnection {

        private var mOperationsServiceBinder: OperationsService.OperationsServiceBinder? = null

        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            if (component == ComponentName(
                    baseActivity,
                    OperationsService::class.java
                )
            ) {
                mOperationsServiceBinder = service as OperationsService.OperationsServiceBinder

                try {

                    val mBaseUrl = serverNextCloud.url
                    val webViewUser = serverNextCloud.username
                    val webViewPassword = serverNextCloud.password
                    checkOcServer(mBaseUrl)
                } catch (e: Exception) {
                    // mServerStatusIcon = R.drawable.ic_alert
                    // mServerStatusText = getString(R.string.qr_could_not_be_read)
                    //  showServerStatus()
                }
            }
        }


        override fun onServiceDisconnected(p0: ComponentName?) {

        }
    }


}