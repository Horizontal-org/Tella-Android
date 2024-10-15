package rs.readahead.washington.mobile.views.dialog.googledrive

import android.content.Context
import android.content.Intent
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.data.googledrive.GoogleDriveRepository
import rs.readahead.washington.mobile.views.fragment.googledrive.di.DriveServiceProvider
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SharedGoogleDriveViewModel @Inject constructor(
    private val repository: GoogleDriveRepository,
    private val driveServiceProvider: DriveServiceProvider
) : ViewModel() {
    fun setEmail(email: String) {
        _email.value = email
    }

    private val _email = MutableLiveData<String?>()
    val email: MutableLiveData<String?> get() = _email
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage
    private val _sharedDrives = MutableLiveData<List<Folder>>()
    val sharedDrives: LiveData<List<Folder>> get() = _sharedDrives
    private val _folderCreated = MutableLiveData<String>()
    val folderCreated: LiveData<String> get() = _folderCreated
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error
    private val _permissionResult = MutableLiveData<Boolean>()
    val permissionResult: LiveData<Boolean> get() = _permissionResult

    // Fetch shared drives using the repository
    private val _authorizationIntent = MutableLiveData<Intent?>()
    val authorizationIntent: LiveData<Intent?> get() = _authorizationIntent

    fun createFolder(googleDriveServer: GoogleDriveServer) {
        viewModelScope.launch {
            try {
                val folderId = withContext(Dispatchers.IO) {
                    repository.createFolder(googleDriveServer)
                }
                _folderCreated.postValue(folderId)

            } catch (e: UserRecoverableAuthIOException) {
                _authorizationIntent.value = e.intent // Pass the authorization intent
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

    fun fetchSharedDrives() {
        viewModelScope.launch {
            try {
                val email = _email.value ?: return@launch
                val sharedDriveList = withContext(Dispatchers.IO) {
                    repository.fetchSharedDrives(email = email)
                }
                _sharedDrives.value = sharedDriveList
            } catch (e: UserRecoverableAuthIOException) {
                _authorizationIntent.value = e.intent // Pass the authorization intent
            } catch (e: Exception) {
                _errorMessage.value = "Failed to retrieve shared drives: ${e.message}"
            }
        }
    }

    fun checkFolderPermissions(folderId: String, email: String) {
        viewModelScope.launch {
            try {
                val driveService = driveServiceProvider.getDriveService(email)
                val permissions = withContext(Dispatchers.IO) {
                    // Fetch permissions for the given folder
                    driveService.permissions().list(folderId).execute()
                }
                var hasWriteAccess = false
                // Check if the user has write or owner access
                for (permission in permissions.permissions) {
                    if (permission.role == "writer" || permission.role == "owner") {
                        hasWriteAccess = true
                        break
                    }
                }
                _permissionResult.postValue(hasWriteAccess) // Post the result to LiveData
            } catch (e: Exception) {
                // Handle errors appropriately
                _errorMessage.postValue("Failed to check folder permissions: ${e.message}")
                _permissionResult.postValue(false) // Post failure to LiveData
            }
        }
    }
}
