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
import timber.log.Timber
import java.io.File
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
            var tempFile: File? = null
            try {
                // Fetch the file from the encrypted vault
                val file = MyApplication.rxVault.getStream(mediaFile)
              //  if (!file.exists()) throw FileNotFoundException("File does not exist: ${file.absolutePath}")

                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mediaFile.mimeType) ?: "bin"
                val fileNameWithExtension = ensureFileHasExtension(mediaFile.name, extension)

                // Create a temporary file
                tempFile = File.createTempFile("upload_", null, context.cacheDir)
                TempFileManager.addFile(tempFile)

                // Copy the decrypted content to the temp file

                    tempFile.outputStream().use { output ->
                        file.copyTo(output)
                    }


                val localFilePath = tempFile.absolutePath
                val remoteFilePath = "$folderPath/$fileNameWithExtension"
                val tempUploadId = UUID.randomUUID().toString()

                val uploadOperation = ChunkedFileUploadRemoteOperation(
                    localFilePath,
                    remoteFilePath,
                    mediaFile.mimeType,
                    tempUploadId,
                    System.currentTimeMillis(),
                    true
                )

                val progressListener = OnDatatransferProgressListener { current, total, _, _ ->
                    val progress = (current.toFloat() / total.toFloat()) * 100
                    val status = if (current == total) UploadProgressInfo.Status.FINISHED else UploadProgressInfo.Status.OK

                    emitter.onNext(
                        UploadProgressInfo(
                            mediaFile,
                            progress.toLong(),
                            status
                        )
                    )
                }

                uploadOperation.addDataTransferProgressListener(progressListener)

                val result = uploadOperation.execute(client)
                when {
                    result.isSuccess -> {
                        emitter.onNext(
                            UploadProgressInfo(
                                mediaFile,
                                tempFile.length(),
                                UploadProgressInfo.Status.FINISHED
                            )
                        )
                        emitter.onComplete()
                    }
                    result.httpCode == 401 -> {
                        emitter.onNext(
                            UploadProgressInfo(
                                mediaFile,
                                0,
                                UploadProgressInfo.Status.UNAUTHORIZED
                            )
                        )
                        emitter.onError(Exception("Unauthorized: ${result.logMessage}"))
                    }
                    result.httpCode == 409 -> {
                        emitter.onNext(
                            UploadProgressInfo(
                                mediaFile,
                                0,
                                UploadProgressInfo.Status.CONFLICT
                            )
                        )
                        emitter.onError(Exception("Conflict: ${result.logMessage}"))
                    }
                    result.httpCode == -1 -> {
                        emitter.onNext(
                            UploadProgressInfo(
                                mediaFile,
                                0,
                                UploadProgressInfo.Status.UNKNOWN_HOST
                            )
                        )
                        emitter.onError(Exception("Unknown host: ${result.logMessage}"))
                    }
                    else -> {
                        emitter.onNext(
                            UploadProgressInfo(
                                mediaFile,
                                0,
                                UploadProgressInfo.Status.ERROR
                            )
                        )
                        emitter.onError(Exception("Upload failed: ${result.logMessage}, Code: ${result.httpCode}"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error uploading file: ${mediaFile.name}")
                emitter.onNext(
                    UploadProgressInfo(
                        mediaFile,
                        0,
                        UploadProgressInfo.Status.ERROR
                    )
                )
                emitter.onError(e)
            } finally {
                // Ensure temp file is deleted after processing
                tempFile?.delete()
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


}