package rs.readahead.washington.mobile.views.dialog.googledrive

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.navigation.fragment.NavHostFragment
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.performFileSearch
import rs.readahead.washington.mobile.views.activity.viewer.VaultActionsHelper.showExportWithMetadataDialog
import rs.readahead.washington.mobile.views.activity.viewer.chosenVaultFile
import rs.readahead.washington.mobile.views.activity.viewer.filePicker
import rs.readahead.washington.mobile.views.activity.viewer.requestPermission
import rs.readahead.washington.mobile.views.activity.viewer.sharedViewModel
import rs.readahead.washington.mobile.views.activity.viewer.withMetadata
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import timber.log.Timber


class GoogleDriveConnectFlowActivity : BaseLockActivity() {

    private lateinit var credentialManager: CredentialManager
    private lateinit var requestAuthorizationLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_drive)
        credentialManager = CredentialManager.create(this)

        val signInWithGoogleOption: GetSignInWithGoogleOption =
            GetSignInWithGoogleOption.Builder("166289458819-43i302vr6n3r62unoboiinq91ccvur3o.apps.googleusercontent.com")
                .build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        // findViewById<Button>(R.id.submit_button).setOnClickListener {
        maybeChangeTemporaryTimeout {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@GoogleDriveConnectFlowActivity
                )
                    handleSignIn(result)  // Process the sign-in result
            } catch (e: Exception) {
                // Handle the error, such as showing a message to the user
            }
        }

        }
        // }
        requestAuthorizationLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    // Retry accessing Google Drive after authorization
                    if (::driveService.isInitialized) {
                      //  maybeChangeTemporaryTimeout {
                            fetchSharedDrives(driveService)
                     //   }
                    } else {
                        Timber.e("Drive service is not initialized")
                    }
                } else {
                    // Handle the case where the user denies the authorization
                    Timber.e("Authorization denied by user.")
                }
            }

    }

    private lateinit var driveService: Drive
    private fun handleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and authenticate on your server.
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        Timber.d("Received an invalid Google ID token response")

                        val idToken = googleIdTokenCredential.idToken
                        Timber.d("Google ID Token: $idToken")

                        // Extract email from the Google ID token
                        val email = getEmailFromIdToken(idToken)

                        // Initialize GoogleAccountCredential
                        val googleAccountCredential = GoogleAccountCredential.usingOAuth2(
                            this, listOf(DriveScopes.DRIVE)
                        ).apply {
                            selectedAccountName = email
                        }

                        // Initialize the Drive service
                        driveService = Drive.Builder(
                            NetHttpTransport(),
                            GsonFactory(),
                            googleAccountCredential
                        ).setApplicationName("Tella").build()

                        // Attempt to fetch shared drives
                        fetchSharedDrives(driveService)
                    } catch (e: UserRecoverableAuthIOException) {
                        // Handle the exception by starting the user consent activity
                        requestAuthorizationLauncher.launch(e.intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to retrieve or process Google ID Token")
                    }
                } else {
                    Timber.e("Credential type is not a Google ID Token Credential")
                }
            }

            else -> {
                Timber.e("Unexpected credential type or no credentials returned")
            }
        }
    }

    private fun fetchSharedDrives(driveService: Drive) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = driveService.Files().list()
                val result = request.execute()
                val sharedDrives = result.files

                if (sharedDrives != null && sharedDrives.isNotEmpty()) {
                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val navController = navHostFragment.navController
                    navController.navigate(R.id.google_drive)
                } else {
                    Timber.d("No shared drives found.")
                }
            } catch (e: UserRecoverableAuthIOException) {
                requestAuthorizationLauncher.launch(e.intent)
                Timber.e(e, "Failed to fetch shared drives.")
            }
        }
    }


    private fun getEmailFromIdToken(idToken: String): String {
        val parts = idToken.split(".")
        val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
        val json = org.json.JSONObject(payload)
        return json.getString("email")
    }

}



