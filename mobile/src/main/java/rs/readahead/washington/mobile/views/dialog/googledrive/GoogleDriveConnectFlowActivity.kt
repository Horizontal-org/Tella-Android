package rs.readahead.washington.mobile.views.dialog.googledrive

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity


class GoogleDriveConnectFlowActivity : BaseLockActivity() {

    private val REQUEST_CODE_SIGN_IN = 1001

    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_drive)

        credentialManager = CredentialManager.create(this)

        val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption.Builder("166289458819-43i302vr6n3r62unoboiinq91ccvur3o.apps.googleusercontent.com")
            .build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        findViewById<Button>(R.id.submit_button).setOnClickListener {

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

//            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
//                .setFilterByAuthorizedAccounts(false)
//                .setAutoSelectEnabled(false)  // Ensure the user is prompted to sign in
//                .setServerClientId("166289458819-43i302vr6n3r62unoboiinq91ccvur3o.apps.googleusercontent.com")
//                .build()
//
//            val request: GetCredentialRequest = GetCredentialRequest.Builder()
//                .addCredentialOption(googleIdOption)
//                .build()
//
//            CoroutineScope(Dispatchers.Main).launch {
//                try {
//                    val result = credentialManager.getCredential(
//                        request = request,
//                        context = this@GoogleDriveConnectFlowActivity,
//                    )
//                    handleSignIn(result)  // Process the sign-in result
//                } catch (e: Exception) {
//                    if (e.message?.contains("No credentials found") == true) {
//                        Toast.makeText(
//                            this@GoogleDriveConnectFlowActivity,
//                            "No credentials found. Please sign in with your Google account.",
//                            Toast.LENGTH_LONG
//                        ).show()
//
//                        if (e.message?.contains("No credentials found") == true) {
//                            handleNoCredentialsFound()
//                          val signInIntent = Identity.getSignInClient(this@GoogleDriveConnectFlowActivity)
//                           startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)
//                        }
//                    } else {
//                        e.printStackTrace()  // For debugging purposes
//                        Toast.makeText(
//                            this@GoogleDriveConnectFlowActivity,
//                            "Sign-in failed: ${e.message}",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                }
//            }
        }
    }
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private fun handleNoCredentialsFound() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("166289458819-43i302vr6n3r62unoboiinq91ccvur3o.apps.googleusercontent.com")
                    .setFilterByAuthorizedAccounts(false)
// Set to false to allow sign-in with new accounts
                    .build()
            )
            .setAutoSelectEnabled(false) // Set to true to auto-select the account if only one exists
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQUEST_CODE_SIGN_IN,
                        null, 0, 0, 0, null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener(this) { e ->
                e.printStackTrace()
                Toast.makeText(this, "Sign-in process failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            try {
                val credential: SignInCredential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                // Handle the sign-in credential
                    // handleSignIn(idToken)
            } catch (e: ApiException) {
                Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        if (result != null) {
           // val idToken = result.data
            //if (idToken != null) {
                // ID Token retrieved, proceed to verify it with your backend
                //verifyIdTokenWithServer(idToken)
//            } else {
//                // Handle the case where the ID token is null, sign-in failed
//                Toast.makeText(this, "Sign-in failed: ID token is null", Toast.LENGTH_LONG).show()
//            }
//        } else {
//            // Handle the case where result is null, sign-in failed
//            Toast.makeText(this, "Sign-in failed: No result returned", Toast.LENGTH_LONG).show()
//        }
    }


    // Example method to verify the ID token with your server
//    private fun verifyIdTokenWithServer(idToken: String) {
//        // Make a network request to your backend server to verify the ID token
//        // This usually involves sending the token to an API endpoint
//        // Here's a simplified example using Retrofit (you can use any HTTP client):
//
//        // Assume you have a Retrofit API service defined
//        val apiService = RetrofitInstance.apiService
//        val call = apiService.verifyIdToken(idToken)
//
//        call.enqueue(object : Callback<VerifyIdTokenResponse> {
//            override fun onResponse(call: Call<VerifyIdTokenResponse>, response: Response<VerifyIdTokenResponse>) {
//                if (response.isSuccessful) {
//                    // Token verified successfully, update UI or proceed to the next step
//                    val user = response.body()?.user
//                    // Save user info locally if needed
//                    saveUserInfo(user)
//                    // Update UI
//                    Toast.makeText(this@GoogleDriveConnectFlowActivity, "Sign-in successful!", Toast.LENGTH_LONG).show()
//                } else {
//                    // Handle the case where token verification failed
//                    Toast.makeText(this@GoogleDriveConnectFlowActivity, "Sign-in failed: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
//                }
//            }
//
//            override fun onFailure(call: Call<VerifyIdTokenResponse>, t: Throwable) {
//                // Handle network or server errors
//                Toast.makeText(this@GoogleDriveConnectFlowActivity, "Sign-in failed: ${t.message}", Toast.LENGTH_LONG).show()
//            }
//        })
//    }

//    // Example method to save user information locally
//    private fun saveUserInfo(user: User?) {
//        // Save user information in shared preferences, a local database, or another storage method
//        // Example:
//        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//        sharedPreferences.edit()
//            .putString("user_id", user?.id)
//            .putString("user_name", user?.name)
//            .putString("user_email", user?.email)
//            .apply()
//    }

    }
}

