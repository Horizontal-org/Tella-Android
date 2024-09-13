package rs.readahead.washington.mobile.domain.repository.googledrive

import android.content.Context
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.api.services.drive.Drive
import io.reactivex.Single
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder

interface GoogleDriveRepositoryInterface {
    suspend fun getCredential(
        request: GetCredentialRequest,
        context: Context
    ): GetCredentialResponse

    // New method to fetch shared drives
    suspend fun fetchSharedDrives(email: String): List<Folder>
    suspend fun createFolder(email: String, folderName: String): String
}