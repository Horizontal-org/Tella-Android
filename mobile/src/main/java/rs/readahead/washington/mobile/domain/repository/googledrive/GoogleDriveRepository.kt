package rs.readahead.washington.mobile.domain.repository.googledrive

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.api.client.http.FileContent
import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.hzontal.tella_vault.rx.RxVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.HttpException
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.repository.SkippableMediaFileRequestBody
import rs.readahead.washington.mobile.domain.entity.googledrive.Folder
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.googledrive.di.DriveServiceProvider
import timber.log.Timber
import java.io.IOException
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
                val query =
                    "mimeType = 'application/vnd.google-apps.folder' and sharedWithMe = true"
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
                val folder =
                    driveServiceProvider.getDriveService(googleDriveServer.username).files()
                        .create(folderMetadata)
                        .setFields("id")
                        .execute()
                folder.id
            } catch (e: Exception) {
                throw e
            }
        }
    }


    suspend fun uploadFiles(
        googleDriveServer: GoogleDriveServer,
        reportInstance: ReportInstance // List of files from the database
    ): List<String> {
        return withContext(Dispatchers.IO) {
            val uploadedFileIds = mutableListOf<String>()

            for (mediaFile in reportInstance.widgetMediaFiles) {
                try {
                    val fileMetadata = File().apply {
                        name = mediaFile.name // Use the file name from FormMediaFile
                        description = mediaFile.id // Set the file description
                        mimeType = mediaFile.mimeType // Set the mime type from the file metadata
                        googleDriveServer.folderId
                    }

                    // Replace FileContent with SkippableMediaFileRequestBody
                    val requestBody = SkippableMediaFileRequestBody(
                        mediaFile, // VaultFile from your code
                        0L, // Set any bytes to skip if needed
                         null // You can pass your own progress listener here
                    )

                    // Upload the file using the request body
                    val uploadedFile = driveServiceProvider.getDriveService(googleDriveServer.username)
                        .files()
                        .create(fileMetadata, InputStreamContent(mediaFile.mimeType, requestBody.publicInputStream))
                        .setFields("id") // Request only the file ID back
                        .execute()

                    Timber.d("File uploaded with ID: ${uploadedFile.id}")
                    uploadedFileIds.add(uploadedFile.id) // Collect the uploaded file ID
                } catch (e: Exception) {
                    Timber.e(e, "Error uploading file: ${mediaFile.name}")
                    throw e // Re-throw the exception to handle it later
                }
            }

            uploadedFileIds // Return the list of uploaded file IDs
        }
    }


}