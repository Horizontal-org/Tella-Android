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
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.HttpException
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.repository.SkippableMediaFileRequestBody
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
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

    fun uploadFilesWithProgress(
        googleDriveServer: GoogleDriveServer,
        reportInstance: ReportInstance
    ): Flowable<UploadProgressInfo> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo> ->
            for (mediaFile in reportInstance.widgetMediaFiles) {
                try {
                    // Prepare metadata for the file upload
                    val fileMetadata = File().apply {
                        name = mediaFile.name
                        description = mediaFile.id
                        mimeType = mediaFile.mimeType
                        parents = listOf(googleDriveServer.folderId) // Specify the parent folder in Google Drive
                    }

                    // Create request body for uploading with progress
                    val requestBody = SkippableMediaFileRequestBody(
                        mediaFile, // The file to be uploaded
                        0L
                    ) // Starting from the beginning
                    { currentProgress: Long, _ ->
                        // Emit progress updates
                        emitter.onNext(
                            UploadProgressInfo(
                                mediaFile,
                                currentProgress,
                                mediaFile.size
                            )
                        )
                    }

                    // Build the file content for Google Drive API
                    val fileContent = InputStreamContent(mediaFile.mimeType, requestBody.getPublicInputStream())

                    // Upload the file to Google Drive
                    val uploadedFile = driveServiceProvider.getDriveService(googleDriveServer.username)
                        .files()
                        .create(fileMetadata, fileContent)
                        .setFields("id") // Request only the file ID back
                        .execute()

                    // Emit final progress as complete
                    emitter.onNext(UploadProgressInfo(mediaFile, mediaFile.size, UploadProgressInfo.Status.FINISHED))

                    // Add uploaded file ID to the list
                    Timber.d("File uploaded with ID: ${uploadedFile.id}")
                } catch (e: Exception) {
                    Timber.e(e, "Error uploading file: ${mediaFile.name}")
                    emitter.onError(e) // Emit error in case of failure
                }
            }
            emitter.onComplete() // Complete the Flowable when all files are uploaded
        }, BackpressureStrategy.BUFFER) // Use BUFFER strategy for backpressure management
    }


}