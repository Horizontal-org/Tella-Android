package rs.readahead.washington.mobile.domain.repository.googledrive

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.readahead.washington.mobile.views.fragment.googledrive.di.DriveServiceProvider
import timber.log.Timber
import javax.inject.Inject

class GoogleDriveRepository @Inject constructor(
    private val credentialManager: CredentialManager,
    private val driveServiceProvider: DriveServiceProvider
) : GoogleDriveRepositoryInterface {

    override suspend fun getCredential(
        request: GetCredentialRequest,
        context: Context
    ): GetCredentialResponse {
        return withContext(Dispatchers.IO) {
            try {
                val result = credentialManager.getCredential(context, request)
                result
            } catch (e: Exception) {
                throw e
            }
        }
    }

    // New method to fetch shared drives
    override suspend fun fetchSharedDrives(email: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val query =
                    "mimeType = 'application/vnd.google-apps.folder' and sharedWithMe = true"
                val request = driveServiceProvider.getDriveService(email).files().list().setQ(query).setFields("files(id, name)")
                val result: FileList = request.execute()
                result.files.map { it.name }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching shared drives")
                throw e
            }
        }
    }

    override suspend fun createFolder(email: String, folderName: String): String {
        val folderMetadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }

        return withContext(Dispatchers.IO) {
            try {
                val folder = driveServiceProvider.getDriveService(email).files().create(folderMetadata)
                    .setFields("id")
                    .execute()
                folder.id
            } catch (e: Exception) {
                throw e
            }
        }
    }

}