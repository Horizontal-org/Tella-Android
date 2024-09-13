package rs.readahead.washington.mobile.views.dialog.googledrive

import android.content.Context
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import rs.readahead.washington.mobile.domain.repository.googledrive.GoogleDriveRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SharedGoogleDriveViewModel @Inject constructor(
    private val repository: GoogleDriveRepository,
) : ViewModel() {

    fun setEmail(email: String) {
        _email.value = email
    }

    private val _signInResult = MutableLiveData<GetCredentialResponse?>()
    val signInResult: LiveData<GetCredentialResponse?> get() = _signInResult

    private val _email = MutableLiveData<String?>()
    val email: MutableLiveData<String?> get() = _email

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _sharedDrives = MutableLiveData<List<String>>()
    val sharedDrives: LiveData<List<String>> get() = _sharedDrives

    private val _folderCreated = MutableLiveData<String>()
    val folderCreated: LiveData<String> get() = _folderCreated

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error
    fun createFolder(folderName: String) {
        viewModelScope.launch {
            try {
                val email = _email.value ?: return@launch
                val folderId = repository.createFolder(email, folderName)
                _folderCreated.value = folderId
                // Handle success
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create folder: ${e.message}"
            }
        }
    }

    // Perform the sign-in process using coroutines
    fun signInWithGoogle(request: GetCredentialRequest, context: Context) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getCredential(request, context)
                }
                handleSignIn(result)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    // Handle the sign-in result
    private fun handleSignIn(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    processGoogleIdTokenCredential(credential)
                } else {
                    _errorMessage.value = "Credential type is not a Google ID Token Credential"
                }
            }

            else -> {
                _errorMessage.value = "Unexpected credential type or no credentials returned"
            }
        }
    }

    // Process Google ID token and extract email
    private fun processGoogleIdTokenCredential(credential: CustomCredential) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            Timber.d("Google ID Token: $idToken")

            val extractedEmail = getEmailFromIdToken(idToken)
            _email.value = extractedEmail
        } catch (e: Exception) {
            _errorMessage.value = "Failed to retrieve or process Google ID Token: ${e.message}"
            Timber.e(e)
        }
    }

    // Extract email from the Google ID token
    private fun getEmailFromIdToken(idToken: String): String {
        val parts = idToken.split(".")
        val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
        val json = JSONObject(payload)
        return json.getString("email")
    }


    // Fetch shared drives using the repository
    fun fetchSharedDrives() {
        viewModelScope.launch {
            try {
                val email = _email.value ?: return@launch
                val sharedDriveList = withContext(Dispatchers.IO) {
                    repository.fetchSharedDrives(email = email)
                }
                _sharedDrives.value = sharedDriveList
            } catch (e: Exception) {
                _errorMessage.value = "Failed to retrieve shared drives: ${e.message}"
            }
        }
    }
}
