package org.horizontal.tella.mobile.views.dialog.googledrive

import android.content.Context
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.domain.entity.googledrive.Folder
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.data.googledrive.GoogleDriveRepository
import org.horizontal.tella.mobile.views.fragment.googledrive.di.DriveServiceProvider
import javax.inject.Inject

/**
 * Stub implementation of SharedGoogleDriveViewModel for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but all operations
 * post error messages since Google Drive is not available in F-Droid builds.
 */
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

    private val _authorizationIntent = MutableLiveData<android.content.Intent?>()
    val authorizationIntent: LiveData<android.content.Intent?> get() = _authorizationIntent

    fun createFolder(googleDriveServer: GoogleDriveServer) {
        viewModelScope.launch {
            _errorMessage.value = "Google Drive is not available in F-Droid builds"
        }
    }

    fun signInWithGoogle(request: GetCredentialRequest, context: Context) {
        viewModelScope.launch {
            _errorMessage.value = "Google Drive is not available in F-Droid builds"
        }
    }

    fun fetchSharedDrives() {
        viewModelScope.launch {
            _errorMessage.value = "Google Drive is not available in F-Droid builds"
            _sharedDrives.value = emptyList()
        }
    }

    fun checkFolderPermissions(folderId: String, email: String) {
        viewModelScope.launch {
            _errorMessage.value = "Google Drive is not available in F-Droid builds"
            _permissionResult.value = false
        }
    }
}




