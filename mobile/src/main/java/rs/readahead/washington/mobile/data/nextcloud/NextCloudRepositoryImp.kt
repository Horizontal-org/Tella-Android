package rs.readahead.washington.mobile.data.nextcloud

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hzontal.tella_vault.rx.RxVault
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ChunkedFileUploadRemoteOperation
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
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
import java.io.FileOutputStream
import java.io.InputStream
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
                val readFolderOperation = ReadFolderRemoteOperation(newFolderPath)
                val readFolderResult = readFolderOperation.execute(client)

                if (!readFolderResult.isSuccess) {

                    val createFolderOperation = CreateFolderRemoteOperation(newFolderPath, true)
                    val folderResult = createFolderOperation.execute(client)

                    if (!folderResult.isSuccess) {
                        emitter.onError(Exception("Failed to create folder: ${folderResult.logMessage}"))
                        return@create
                    }
                }

                val descriptionFile = File.createTempFile("description", ".txt").apply {
                    writeText(description)
                }

                val descriptionFilePath = "$newFolderPath/description.txt"

                val checkFileOperation = ReadFolderRemoteOperation(descriptionFilePath)
                val checkFileResult = checkFileOperation.execute(client)

                if (checkFileResult.isSuccess) {
                     val deleteOperation = RemoveFileRemoteOperation(descriptionFilePath)
                     deleteOperation.execute(client)
                }

                val uploadOperation = UploadFileRemoteOperation(
                    descriptionFile.absolutePath,
                    descriptionFilePath,
                    "text/plain",
                    0
                )

                val result = uploadOperation.execute(client)

                if (result.isSuccess) {
                    emitter.onSuccess(descriptionFilePath)
                } else {
                    emitter.onError(Exception("Failed to upload description file: ${result.logMessage}"))
                }

            } catch (e: Exception) {
                emitter.onError(Exception("Error uploading description to Nextcloud: ${e.message}", e))
            }
        }
    }



    //TODO IS CREATING A TEMP FILE A SECURE SOLUTION?
    private fun createTempFileFromStream(inputStream: InputStream, mediaFile: FormMediaFile): File {
        val tempFile = File.createTempFile(mediaFile.name, null)
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }

    override fun uploadFileWithProgress(
        client: OwnCloudClient,
        folderPath: String,
        mediaFile: FormMediaFile
    ): Flowable<UploadProgressInfo> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo> ->
            try {


                val file = MyApplication.rxVault.getFile(mediaFile.vaultFile)

                val filePath = "$folderPath/${mediaFile.name}"
                val tempUploadId =
                    UUID.randomUUID().toString()
                val fileSize = file.length()

                // Initialize the chunked upload operation
                val uploadOperation = ChunkedFileUploadRemoteOperation(
                    file.absolutePath,
                    filePath,
                    mediaFile.mimeType,
                    null,
                    System.currentTimeMillis(),
                    true
                )

                // Set up progress listener
                /*   uploadOperation.setOnProgressListener { currentBytes, totalBytes ->
                       // Emit the current progress to the Flowable
                       emitter.onNext(
                           UploadProgressInfo(
                               mediaFile,
                               currentBytes,
                               totalBytes,
                               UploadProgressInfo.Status.IN_PROGRESS
                           )
                       )
                   }*/

                // Execute the upload operation
                val result = uploadOperation.execute(client)
                if (result.isSuccess) {
                    // Upload is successful, emit finished status
                    emitter.onNext(
                        UploadProgressInfo(
                            mediaFile,
                            fileSize,
                            UploadProgressInfo.Status.FINISHED
                        )
                    )
                    emitter.onComplete() // Complete the flowable
                } else {
                    // Handle error
                    emitter.onError(Exception("Chunked file upload failed: ${result.logMessage}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error uploading file in chunks: ${mediaFile.name}")
                emitter.onError(e) // Emit an error if something goes wrong
            }
        }, BackpressureStrategy.LATEST)
    }

    //TODO NEXT STEPS

    // 1 create an overwrite method to renamte files if exist
    // 2 upload files we can get the files from rx but those files are encryoted ??
    // 3 if we use chunck to send the files can we use ReadOperation to get the latest statu of the submitted file?
    // NB : are we going to submit those files undert the created parent folder ? how ?
    // We should find a way to tack the progress chuck operations doesnt seems to have one

    // -> now we are able to sumbit the decription and create it's folder ? --- Ahlem

}