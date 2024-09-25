package rs.readahead.washington.mobile.domain.repository.googledrive

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
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
    override suspend fun fetchSharedDrives(email: String): List<Folder> {
        return withContext(Dispatchers.IO) {
            try {
                val query = "mimeType = 'application/vnd.google-apps.folder' and sharedWithMe = true"
                val request = driveServiceProvider.getDriveService(email)
                    .files()
                    .list()
                    .setQ(query)
                    .setFields("files(id, name)")

                val result: FileList = request.execute()

                // Map the result to a list of Folder objects
                result.files.map { file -> Folder(file.id, file.name) }

            } catch (e: Exception) {
                Timber.e(e, "Error fetching shared drives")
                throw e
            }
        }
    }

    override suspend fun createFolder(googleDriveServer: GoogleDriveServer): String {
        val folderMetadata = File().apply {
            name = googleDriveServer.folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        return withContext(Dispatchers.IO) {
            try {
                val folder = driveServiceProvider.getDriveService(googleDriveServer.username).files().create(folderMetadata)
                    .setFields("id")
                    .execute()
                folder.id
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun uploadFile(
        googleDriveServer: GoogleDriveServer?,
        localFile: java.io.File, // This is a java.io.File for the local file
        folderId: String? = null, // ID of the folder to upload into
        title: String,            // Title of the file
        description: String        // Description of the file
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Create the metadata for the Google Drive file
                val fileMetadata = File().apply {
                    name = title // Set the title as the file name
                  //  description = description // Add the description to metadata
                    mimeType = "image/jpeg" // Adjust this based on the file type (e.g., "application/pdf")
                    folderId?.let { parents = listOf(it) } // Add the file to the folder if folderId is provided
                }

                // Create the FileContent for the upload
                val fileContent = FileContent("image/jpeg", localFile) // Using the local file here

                // Execute the upload request
                val uploadedFile = driveServiceProvider.getDriveService("Lakwafa@gmail.com")
                    .files()
                    .create(fileMetadata, fileContent)
                    .setFields("id") // Only get the file ID back
                    .execute()

                Timber.d("File uploaded with ID: ${uploadedFile.id}")
                uploadedFile.id // Return the file ID
            } catch (e: Exception) {
                Timber.e(e, "Error uploading file")
                throw e
            }
        }
    }

}