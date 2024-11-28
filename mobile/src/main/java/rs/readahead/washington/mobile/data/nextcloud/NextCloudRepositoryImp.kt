package rs.readahead.washington.mobile.data.nextcloud

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.MimeTypeMap
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ChunkedFileUploadRemoteOperation
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.repository.nextcloud.NextCloudRepository
import rs.readahead.washington.mobile.media.MediaFileHandler
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID


class NextCloudRepositoryImp(private val context: Context) : NextCloudRepository {

    override fun validateServerUrl(serverUrl: String): Single<Boolean> {
        return Single.create { emitter ->
            try {
                val uri = Uri.parse(serverUrl)
                val client = OwnCloudClientFactory.createOwnCloudClient(uri, context, false)

                val getCapabilitiesOperation = GetCapabilitiesRemoteOperation()
                getCapabilitiesOperation.execute(
                    client, { _, result ->

                        result?.let {
                            if (it.isSuccess) {
                                emitter.onSuccess(true)
                            } else {
                                emitter.onError(result.exception)
                            }
                        } ?: emitter.onSuccess(false)
                    }, Handler(Looper.getMainLooper())
                )

            } catch (e: Exception) {
                Log.e("ValidateServerUrl", "Exception occurred: ${e.localizedMessage}", e)
                emitter.onError(e)
            }
        }.subscribeOn(Schedulers.io())
    }

    override fun checkUserCredentials(
        serverUrl: String, username: String, password: String
    ): Single<RemoteOperationResult<UserInfo?>> {

        return Single.create<RemoteOperationResult<UserInfo?>> { emitter ->
            try {
                val baseUri = Uri.parse(serverUrl)
                val credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password)

                if (credentials == null) {
                    emitter.onError(Exception("Client or credentials are null"))
                    return@create
                }

                val nextcloudClient = OwnCloudClientFactory.createNextcloudClient(
                    baseUri, credentials.username, credentials.toOkHttpCredentials(), context, true
                )

                // Use a valid method to execute the operation
                val result = GetUserInfoRemoteOperation().execute(nextcloudClient)
                if (result.isSuccess) {
                    emitter.onSuccess(result)
                } else {
                    emitter.onError(result.exception ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun uploadDescription(
        client: OwnCloudClient, folderPath: String, newFolderPath: String, description: String
    ): Single<String> {
        return Single.create { emitter ->
            try {
                val capabilitiesOperation = GetCapabilitiesRemoteOperation()
                val capabilitiesResult = capabilitiesOperation.execute(client)
                if (!capabilitiesResult.isSuccess) {
                    emitter.onError(Exception("Server capabilities could not be fetched: ${capabilitiesResult.logMessage}"))
                    return@create
                }

                val finalFolderPath = generateUniqueFolderName(client, folderPath, newFolderPath)

                val createFolderOperation = CreateFolderRemoteOperation(finalFolderPath, true)
                val folderResult = createFolderOperation.execute(client)
                if (!folderResult.isSuccess) {
                    emitter.onError(Exception("Failed to create folder: ${folderResult.logMessage}"))
                    return@create
                }

                val descriptionFile = File.createTempFile("description", ".txt").apply {
                    writeText(description)
                }

                val descriptionFilePath = "$finalFolderPath/description.txt"

                val timeStamp: Long = System.currentTimeMillis()
                val uploadOperation = UploadFileRemoteOperation(
                    descriptionFile.absolutePath, descriptionFilePath, "text/plain", timeStamp
                )
                val result = uploadOperation.execute(client)

                if (result.isSuccess) {
                    emitter.onSuccess(finalFolderPath)
                } else {
                    emitter.onError(Exception("Failed to upload description file: ${result.logMessage}"))
                }

            } catch (e: Exception) {
                emitter.onError(
                    Exception(
                        "Error uploading description to Nextcloud: ${e.message}", e
                    )
                )
            }
        }
    }

    private fun generateUniqueFolderName(
        client: OwnCloudClient, folderPath: String, newFolderPath: String
    ): String {
        var resultFolderPath = "$folderPath/$newFolderPath"
        var index = 1

        while (true) {
            // Check if the folder exists
            val readFolderOperation = ReadFolderRemoteOperation(resultFolderPath)
            val readFolderResult = readFolderOperation.execute(client)

            if (!readFolderResult.isSuccess) {
                // Folder does not exist; return the current path
                break
            }

            // Folder exists; append or update the index to create a unique name
            resultFolderPath = "$folderPath/${newFolderPath} $index"
            index++
        }

        return resultFolderPath
    }


    override fun uploadFileWithProgress(
        client: OwnCloudClient,
        folderPath: String,
        mediaFile: FormMediaFile
    ): Flowable<UploadProgressInfo> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo> ->
            try {
                // Fetch the local file from the vault
                val file = MyApplication.rxVault.getFile(mediaFile)
                if (!file.exists()) {
                    throw FileNotFoundException("File does not exist: ${file.absolutePath}")
                }

                // Determine the file name with the correct extension
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mediaFile.mimeType)
                    ?: "bin" // Default fallback extension
                val fileNameWithExtension = ensureFileHasExtension(mediaFile.name, extension)

                // Define paths
                val localFilePath = File(file.parentFile, fileNameWithExtension).absolutePath
                val remoteFilePath = "$folderPath/$fileNameWithExtension"
                val fileSize = file.length()
                val tempUploadId = UUID.randomUUID().toString()

                // Initialize the chunked upload operation
                val uploadOperation = UploadFileRemoteOperation(
                    localFilePath,
                    remoteFilePath,
                    mediaFile.mimeType,
                    System.currentTimeMillis(),
                )

                val progressListener =
                    OnDatatransferProgressListener { current, total, speed, fileName ->
                        val progress = (current.toFloat() / total.toFloat()) * 100
                        // Emit progress to Flowable
                        emitter.onNext(
                            UploadProgressInfo(
                                mediaFile,
                                progress.toLong(),
                                UploadProgressInfo.Status.STARTED
                            )
                        )
                    }

                // Add the progress listener to the upload operation
                uploadOperation.addDataTransferProgressListener(progressListener)

                // Execute the upload operation and handle progress
                val result = uploadOperation.execute(client)
                if (result.isSuccess) {
                    // Upload finished, emit final progress
                    emitter.onNext(
                        UploadProgressInfo(
                            mediaFile,
                            fileSize,
                            UploadProgressInfo.Status.FINISHED
                        )
                    )
                    emitter.onComplete()
                } else {
                    emitter.onError(
                        Exception("Chunked file upload failed: ${result.logMessage}")
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error uploading file in chunks: ${mediaFile.name}")
                emitter.onError(e)
            }
        }, BackpressureStrategy.LATEST)
    }


    /**
     * Ensures that the file name has the correct extension.
     * Appends the given extension if it's not already present.
     */
    private fun ensureFileHasExtension(fileName: String, extension: String): String {
        return if (fileName.endsWith(".$extension", ignoreCase = true)) {
            fileName
        } else {
            "$fileName.$extension"
        }
    }

    //TODO NEXT STEPS

    // 1 create an overwrite method to renamte files if exist
    // 2 upload files we can get the files from rx but those files are encryoted ??
    // 3 if we use chunck to send the files can we use ReadOperation to get the latest statu of the submitted file?
    // NB : are we going to submit those files undert the created parent folder ? how ?
    // We should find a way to tack the progress chuck operations doesnt seems to have one

    // -> now we are able to sumbit the decription and create it's folder ? --- Ahlem

}