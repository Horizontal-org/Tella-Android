package rs.readahead.washington.mobile.domain.repository.googledrive

import android.content.Context
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.api.services.drive.Drive
import io.reactivex.Single

interface GoogleDriveRepositoryInterface {
    suspend fun getCredential(request: GetCredentialRequest, context: Context): GetCredentialResponse
    // New method to fetch shared drives
    suspend fun fetchSharedDrives(driveService: Drive): List<String>
}