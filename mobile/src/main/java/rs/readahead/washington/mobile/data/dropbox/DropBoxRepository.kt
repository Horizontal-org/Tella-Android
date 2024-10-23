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

    override fun createDropboxFolder(
        client: DbxClientV2,
        folderName: String,
        description: String // Add description parameter
    ): Single<String> {
        return Single.create { emitter ->
            try {
                // Create the folder path
                val folderPath = "/$folderName"
                val folderMetadata = client.files().createFolderV2(folderPath, true)

                // Emit the folder path on success or handle possible null value
                folderMetadata.metadata.pathLower?.let { path ->
                    try {
                        // Create description file content
                        val descriptionFileName = "description.txt"
                        val descriptionFilePath = "$folderPath/$descriptionFileName"
                        val descriptionInputStream = description.byteInputStream()

                        // Upload the description file to Dropbox
                        client.files().uploadBuilder(descriptionFilePath)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(descriptionInputStream)

                        // Emit success after creating folder and description file
                        emitter.onSuccess(path)
                    } catch (e: Exception) {
                        // Handle error while uploading description file
                        emitter.onError(Exception("Failed to create description file: ${e.message}", e))
                    }
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
        client: DbxClientV2,
        folderPath: String,
        mediaFile: FormMediaFile,
    ): Flowable<UploadProgressInfo> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo> ->
            try {
                // Create the request body with progress tracking
                val requestBody = SkippableMediaFileRequestBody(
                    mediaFile,
                    0L // Start from the beginning
                ) { currentProgress: Long, _ ->
                    emitter.onNext(UploadProgressInfo(mediaFile, currentProgress, mediaFile.size))
                }

                val bufferSize = 1024 * 1024 // 1MB buffer for efficient reading
                val inputStream = requestBody.publicInputStream.buffered(bufferSize)

                // Full file path in Dropbox
                val filePath = "$folderPath/${mediaFile.name}"

                // Create upload builder for Dropbox
                val uploadBuilder = client.files()
                    .uploadBuilder(filePath)
                    .withMode(WriteMode.OVERWRITE)


                // Track time for emitting progress updates
                var lastEmittedTime = 0L

                // Upload file and track progress
                uploadBuilder.uploadAndFinish(inputStream) { bytesUploaded: Long ->
                    val fileSize = mediaFile.size

                    // Emit progress updates every 500ms
                    if (System.currentTimeMillis() - lastEmittedTime > 500) {
                        lastEmittedTime = System.currentTimeMillis()
                        emitter.onNext(UploadProgressInfo(mediaFile, bytesUploaded, fileSize))
                    }
                }

                // Emit final progress and completion
                emitter.onNext(
                    UploadProgressInfo(
                        mediaFile,
                        mediaFile.size,
                        UploadProgressInfo.Status.FINISHED
                    )
                )
                emitter.onComplete()

            } catch (e: Exception) {
                // Handle errors
                Timber.e(e, "Error uploading file: ${mediaFile.name}")
                emitter.onError(e)
            }
        }, BackpressureStrategy.LATEST) // Emit only the latest progress update
    }

}