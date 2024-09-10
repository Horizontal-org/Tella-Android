import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class SharedGoogleDriveViewModel : ViewModel() {

    private val _signInResult = MutableLiveData<GetCredentialResponse?>()
    val signInResult: LiveData<GetCredentialResponse?> get() = _signInResult

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> get() = _email

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _signInError = MutableLiveData<String?>()
    val signInError: LiveData<String?> = _signInError

    private val _sharedDrives = MutableLiveData<List<String>>()
    val sharedDrives: LiveData<List<String>> get() = _sharedDrives

    private lateinit var driveService: Drive

    // Perform the sign-in process
    fun signInWithGoogle(
        credentialManager: CredentialManager,
        request: GetCredentialRequest,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val result = credentialManager.getCredential(context, request)
                _signInResult.value = result
                result?.let {
                    handleSignIn(it)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to retrieve or process Google ID Token")
                _errorMessage.value = e.message
            }
        }
    }

    // Handle the sign-in result
    private fun handleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is GoogleIdTokenCredential -> {
                try {
                    val idToken = credential.idToken
                    Timber.d("Google ID Token: $idToken")

                    // Extract email from the Google ID token
                    val email = getEmailFromIdToken(idToken)
                    _email.value = email

                    // You can navigate to the next fragment by passing the email
                    // through LiveData or use a callback to notify the fragment.
                } catch (e: Exception) {
                    Timber.e(e, "Failed to retrieve or process Google ID Token")
                    _errorMessage.value = e.message
                }
            }

            else -> {
                Timber.e("Unexpected credential type or no credentials returned")
                _errorMessage.value = "Unexpected credential type or no credentials returned"
            }
        }
    }

    // Extract email from the Google ID token
    private fun getEmailFromIdToken(idToken: String): String {
        val parts = idToken.split(".")
        val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
        val json = JSONObject(payload)
        return json.getString("email")
    }

    fun setEmail(email: String) {
        _email.value = email
    }

    fun initializeDriveService(context: Context) {
        val googleAccountCredential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE)
        ).apply {
            selectedAccountName = _email.value
        }

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            googleAccountCredential
        ).setApplicationName("Tella").build()
    }
    fun fetchSharedDrives() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val query =
                    "mimeType = 'application/vnd.google-apps.folder' and sharedWithMe = true"
                val request = driveService.files().list().setQ(query).setFields("files(id, name)")
                val result: FileList = request.execute()
                _sharedDrives.postValue(result.files.map { it.name })
            } catch (e: Exception) {
                _signInError.postValue("Failed to retrieve Google ID Token: ${e.message}")
            }
        }
    }
}
