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
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import timber.log.Timber


class GoogleDriveConnectFlowActivity : BaseActivity() {

    private lateinit var credentialManager: CredentialManager

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

    }

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

                        val navHostFragment =
                            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                        val navController = navHostFragment.navController
                        navController.navigate(R.id.google_drive)

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



    private fun getEmailFromIdToken(idToken: String): String {
        val parts = idToken.split(".")
        val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
        val json = org.json.JSONObject(payload)
        return json.getString("email")
    }

}



