package rs.readahead.washington.mobile.data.nextcloud

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.UserInfo
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
                    client,
                    { _, result ->

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
        serverUrl: String,
        username: String,
        password: String
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
                    baseUri,
                    credentials.username,
                    credentials.toOkHttpCredentials(),
                    context,
                    true
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
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun uploadDescription(
        client: OwnCloudClient,
        folderPath: String,
        newFolderPath: String,
        description: String
    ): Single<String> {
        return Single.create { emitter ->
            try {
                // Validate client configuration
                val capabilitiesOperation = GetCapabilitiesRemoteOperation()
                val capabilitiesResult = capabilitiesOperation.execute(client)
                if (!capabilitiesResult.isSuccess) {
                    emitter.onError(Exception("Server capabilities could not be fetched: ${capabilitiesResult.logMessage}"))
                    return@create
                }

                // Check if folder exists
                val readFolderOperation =
                    ReadFolderRemoteOperation("$folderPath/$newFolderPath")
                val readFolderResult = readFolderOperation.execute(client)

                if (!readFolderResult.isSuccess) {
                    // Create folder if it doesn't exist
                    val createFolderOperation = CreateFolderRemoteOperation(
                        "$folderPath/$newFolderPath",
                        true
                    )
                    val folderResult = createFolderOperation.execute(client)

                    if (!folderResult.isSuccess) {
                        emitter.onError(Exception("Failed to create folder: ${folderResult.logMessage}"))
                        return@create
                    }
                }

                //ELSE RENAME

                // Prepare the description file
                val descriptionFile = File.createTempFile("description", ".txt").apply {
                    writeText(description)
                }

                val rootPath =
                    "$folderPath/$newFolderPath"
                val descriptionFilePath = "$rootPath/description.txt"

                // Check if the file already exists and delete it
                /*  val checkFileOperation = ReadFolderRemoteOperation(newFolderPath)
                  val checkFileResult = checkFileOperation.execute(client)

                  if (checkFileResult.isSuccess) {
                      val existingFile = checkFileResult.data
                          ?.firstOrNull { it == descriptionFilePath }
                      if (existingFile != null) {
                          val deleteOperation = RemoveFileRemoteOperation(descriptionFilePath)
                          val deleteResult = deleteOperation.execute(client)
                          if (!deleteResult.isSuccess) {
                              emitter.onError(Exception("Failed to delete existing description file: ${deleteResult.logMessage}"))
                              return@create
                          }
                      }
                  }*/

                // Upload the description file
                val timeStamp: Long = System.currentTimeMillis()
                val uploadOperation = UploadFileRemoteOperation(
                    descriptionFile.absolutePath,
                    descriptionFilePath,
                    "text/plain",
                    timeStamp
                )
                val result = uploadOperation.execute(client)
                // Get the last modification date of the file from the file system

                if (result.isSuccess) {
                    emitter.onSuccess(rootPath)
                } else {
                    emitter.onError(Exception("Failed to upload description file: ${result.logMessage}"))
                }

            } catch (e: Exception) {
                emitter.onError(
                    Exception(
                        "Error uploading description to Nextcloud: ${e.message}",
                        e
                    )
                )
            }
        }
    }


    override fun uploadFileWithProgress(
        client: OwnCloudClient,
        folderPath: String,
        mediaFile: FormMediaFile
    ): Flowable<UploadProgressInfo> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo> ->
            try {
                val file = MyApplication.rxVault.getFile(mediaFile.vaultFile)
                val fileSize = file.length()
                val filePath = "$folderPath/${mediaFile.name}"
                val tempUploadId = UUID.randomUUID().toString()

                // Check if file already exists and handle renaming
                /*  val readFolderOperation = ReadFolderRemoteOperation(folderPath)
                  val folderResult = readFolderOperation.execute(client)
                  if (!folderResult.isSuccess) {
                      emitter.onError(
                          Exception("Failed to read folder: ${folderResult.logMessage}")
                      )
                      return@create
                  }

                  val existingFiles = folderResult.resultData
                  val existingFile = existingFiles.find { it.remotePath == filePath }
                  val finalFilePath = if (existingFile != null) {
                      "$folderPath/${mediaFile.name}_${System.currentTimeMillis()}${mediaFile.mimeType}"
                  } else {
                      filePath
                  }*/

                // Initialize the chunked upload operation
                val uploadOperation = ChunkedFileUploadRemoteOperation(
                    file.absolutePath,
                    filePath,
                    mediaFile.mimeType,
                    tempUploadId,
                    System.currentTimeMillis(),
                    true
                )

                // Set up progress listener
                /*    uploadOperation.on { currentBytes, totalBytes ->
                        val progressInfo = UploadProgressInfo(
                            mediaFile,
                            currentBytes,
                            totalBytes,
                            UploadProgressInfo.Status.IN_PROGRESS
                        )
                        emitter.onNext(progressInfo)
                    }*/

                // Execute the upload operation
                val result = uploadOperation.execute(client)
                if (result.isSuccess) {
                    // Emit success status
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
                emitter.onError(e) // Emit an error if something goes wrong
            }
        }, BackpressureStrategy.LATEST)
    }


    fun createFileName(fileNameBase: String): String {
        var resultFileName = fileNameBase

        fun generateNewFileName() {
            val nameWithoutExtension = resultFileName.substringBeforeLast('.', resultFileName)
            val extension = resultFileName.substringAfterLast('.', "")

            val characters = nameWithoutExtension.toCharArray()
            if (characters.size < 2) {
                resultFileName = if (extension.isEmpty()) {
                    "$nameWithoutExtension 1"
                } else {
                    "$nameWithoutExtension 1.$extension"
                }
            } else {
                val secondLastChar = characters[characters.size - 2]
                val lastChar = characters.last()

                val num = lastChar.toString().toIntOrNull()
                if (secondLastChar == ' ' && num != null) {
                    val newName = nameWithoutExtension.dropLast(1) + (num + 1)
                    resultFileName = if (extension.isEmpty()) {
                        newName
                    } else {
                        "$newName.$extension"
                    }
                } else {
                    resultFileName = if (extension.isEmpty()) {
                        "$nameWithoutExtension 1"
                    } else {
                        "$nameWithoutExtension 1.$extension"
                    }
                }
            }
        }

        // Rename the file as needed
        generateNewFileName()

        return resultFileName
    }

    //TODO NEXT STEPS

    // 1 create an overwrite method to renamte files if exist
    // 2 upload files we can get the files from rx but those files are encryoted ??
    // 3 if we use chunck to send the files can we use ReadOperation to get the latest statu of the submitted file?
    // NB : are we going to submit those files undert the created parent folder ? how ?
    // We should find a way to tack the progress chuck operations doesnt seems to have one

    // -> now we are able to sumbit the decription and create it's folder ? --- Ahlem

}