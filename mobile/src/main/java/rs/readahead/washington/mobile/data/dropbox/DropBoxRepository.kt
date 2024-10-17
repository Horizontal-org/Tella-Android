package rs.readahead.washington.mobile.data.dropbox

import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.Single
import rs.readahead.washington.mobile.data.repository.SkippableMediaFileRequestBody
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.domain.exception.InvalidTokenException
import rs.readahead.washington.mobile.domain.repository.dropbox.IDropBoxRepository
import timber.log.Timber
import javax.inject.Inject

class DropBoxRepository @Inject constructor() : IDropBoxRepository {
    override fun createDropboxClient(server: DropBoxServer): Single<DbxClientV2> {
        return Single.create { emitter ->
            try {
                val accessToken = server.token
                val config = DbxRequestConfig.newBuilder("dropbox/tella").build()
                val dbxClient = DbxClientV2(config, accessToken)
                val account = dbxClient.users().currentAccount

                if (account != null) {
                    emitter.onSuccess(dbxClient)
                } else {
                    emitter.onError(InvalidTokenException("Dropbox token is invalid or expired"))
                }
            } catch (e: InvalidTokenException) {
                emitter.onError(e)
            } catch (e: DbxException) {
                emitter.onError(InvalidTokenException("Dropbox token is invalid or expired"))
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    override fun createDropboxFolder(client: DbxClientV2, folderName: String): Single<String> {
        return Single.create { emitter ->
            try {
                // Create the folder path
                val folderPath = "/$folderName"
                val folderMetadata = client.files().createFolderV2(folderPath, true)

                // Emit the folder path on success or handle possible null value
                folderMetadata.metadata.pathLower?.let { path ->
                    emitter.onSuccess(path)
                } ?: run {
                    emitter.onError(Exception("Failed to create folder: Path is null"))
                }
            } catch (e: DbxException) {
                // Handle Dropbox-specific exceptions
                emitter.onError(Exception("Dropbox error: ${e.message}", e))
            } catch (e: Exception) {
                // Handle general exceptions
                emitter.onError(Exception("Unexpected error: ${e.message}", e))
            }
        }
    }

    override fun uploadFileWithProgress(
        client: DbxClientV2, folderPath: String, mediaFile: FormMediaFile
    ): Flowable<UploadProgressInfo> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo> ->

            try {
                // Prepare the InputStream for the file
                val requestBody = SkippableMediaFileRequestBody(
                    mediaFile, // The file to be uploaded
                    0L
                ) // Starting from the beginning
                { currentProgress: Long, _ ->
                    // Emit progress updates
                    emitter.onNext(UploadProgressInfo(mediaFile, currentProgress, mediaFile.size))
                }

                // Create the full path for the file in Dropbox
                val filePath = "$folderPath/${mediaFile.name}" // Full path for the file

                // Create an upload builder for the file
                val uploadBuilder =
                    client.files().uploadBuilder(filePath).withMode(WriteMode.OVERWRITE)

                // Upload the file using the request body
                uploadBuilder.uploadAndFinish(requestBody.publicInputStream)

                // Emit final progress for this file as complete emitter.onNext(
                UploadProgressInfo(
                    mediaFile, mediaFile.size, UploadProgressInfo.Status.FINISHED
                )

                // Complete the Flowable emission
                emitter.onComplete()
            } catch (e: Exception) {
                // Emit error in case of failure
                Timber.e(e, "Error uploading file: ${mediaFile.name}")
                emitter.onError(e)
            }

        }, BackpressureStrategy.BUFFER) // Use BUFFER strategy for backpressure management
    }

}