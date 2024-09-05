package rs.readahead.washington.mobile.views.dialog.googledrive.setp0

import android.os.Bundle
import android.view.View
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentConnectGoogleDriveBinding
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment
import timber.log.Timber

class ConnectGoogleDriveFragment :
    BaseBindingFragment<FragmentConnectGoogleDriveBinding>(FragmentConnectGoogleDriveBinding::inflate) {
    private lateinit var credentialManager: CredentialManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        credentialManager = context?.let { CredentialManager.create(it) }!!

        val signInWithGoogleOption: GetSignInWithGoogleOption =
            GetSignInWithGoogleOption.Builder("166289458819-43i302vr6n3r62unoboiinq91ccvur3o.apps.googleusercontent.com")
                .build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        baseActivity.maybeChangeTemporaryTimeout {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = this@ConnectGoogleDriveFragment.context?.let {
                        credentialManager.getCredential(
                            request = request,
                            context = it
                        )
                    }
                    if (result != null) {
                        handleSignIn(result)
                    }  // Process the sign-in result
                } catch (e: Exception) {
                    // Handle the error, such as showing a message to the user
                    Timber.e(e, "Failed to retrieve or process Google ID Token")

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
                        // Create a Bundle with the email
                        val args = Bundle().apply {
                            putString("email_key", email)
                        }

                        // Navigate to the target Fragment with the Bundle
                        nav().navigate(
                            R.id.action_googleDriveConnectFragment_to_selectGoogleDriveFragment, // Replace with your action ID
                            args // Pass the Bundle containing the email
                        )

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

