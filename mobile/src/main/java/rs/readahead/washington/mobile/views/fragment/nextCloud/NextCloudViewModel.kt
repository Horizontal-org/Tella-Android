package rs.readahead.washington.mobile.views.fragment.nextCloud

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.NextCloudDataSource
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.repository.nextcloud.NextCloudRepository
import rs.readahead.washington.mobile.domain.usecases.nextcloud.DeleteReportUseCase
import rs.readahead.washington.mobile.domain.usecases.nextcloud.GetReportBundleUseCase
import rs.readahead.washington.mobile.domain.usecases.nextcloud.GetReportsServersUseCase
import rs.readahead.washington.mobile.domain.usecases.nextcloud.GetReportsUseCase
import rs.readahead.washington.mobile.domain.usecases.nextcloud.SaveReportFormInstanceUseCase
import rs.readahead.washington.mobile.media.MediaFileHandler
import rs.readahead.washington.mobile.util.StatusProvider
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportCounts
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import timber.log.Timber
import java.io.File
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class NextCloudViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportsUseCase: GetReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase,
    private val nextCloudRepository: NextCloudRepository,
    private val nextCloudDataSource: NextCloudDataSource,
    private val statusProvider: StatusProvider,
    @ApplicationContext private val context: Context,
) : BaseReportsViewModel() {

    protected val _reportProcess = MutableLiveData<Pair<UploadProgressInfo, ReportInstance>>()
    val reportProcess: LiveData<Pair<UploadProgressInfo, ReportInstance>> get() = _reportProcess

    protected val _instanceProgress = MutableLiveData<ReportInstance>()
    val instanceProgress: MutableLiveData<ReportInstance> get() = _instanceProgress

    override fun deleteReport(instance: ReportInstance) {
        _progress.postValue(true)
        deleteReportUseCase.setId(instance.id)

        deleteReportUseCase.execute(onSuccess = {
            _instanceDeleted.postValue(instance.title)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    override fun getReportBundle(instance: ReportInstance) {
        _progress.postValue(true)
        getReportBundleUseCase.setId(instance.id)
        getReportBundleUseCase.execute(onSuccess = { result ->
            val resultInstance = result.instance
            disposables.add(nextCloudDataSource.getReportMediaFiles(result.instance)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe({ files ->
                    val vaultFiles: MutableList<VaultFile?> =
                        MyApplication.rxVault.get(result.fileIds).blockingGet() ?: return@subscribe
                    val filesResult = arrayListOf<FormMediaFile>()

                    files.forEach { formMediaFile ->
                        val vaultFile =
                            vaultFiles.firstOrNull { vaultFile -> formMediaFile.id == vaultFile?.id }
                        if (vaultFile != null) {
                            val fileResult = FormMediaFile.fromMediaFile(vaultFile)
                            fileResult.status = formMediaFile.status
                            fileResult.uploadedSize = formMediaFile.uploadedSize
                            filesResult.add(fileResult)

                        }
                    }
                    resultInstance.widgetMediaFiles = filesResult
                    _reportInstance.postValue(resultInstance)
                }) { throwable: Throwable? ->
                    Timber.d(throwable)
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                })

        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    override fun getFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long?,
        reportApiId: String,
        status: EntityStatus
    ): ReportInstance {
        return ReportInstance(
            id = id ?: 0L,
            title = title,
            reportApiId = reportApiId,
            description = description,
            status = status,
            widgetMediaFiles = files ?: emptyList(),
            formPartStatus = FormMediaFileStatus.NOT_SUBMITTED,
            serverId = server.id
        )
    }

    override fun getDraftFormInstance(
        title: String, description: String, files: List<FormMediaFile>?, server: Server, id: Long?
    ): ReportInstance {
        return ReportInstance(
            id = id ?: 0L,
            title = title,
            description = description,
            status = EntityStatus.DRAFT,
            widgetMediaFiles = files ?: emptyList(),
            formPartStatus = FormMediaFileStatus.NOT_SUBMITTED,
            serverId = server.id
        )
    }

    override fun listSubmitted() {
        _progress.postValue(true)
        getReportsUseCase.setEntityStatus(EntityStatus.SUBMITTED)
        getReportsUseCase.execute(onSuccess = { result ->
            val resultList = mutableListOf<ViewEntityTemplateItem>()
            result.map { instance ->
                resultList.add(
                    instance.toViewEntityInstanceItem(onOpenClicked = { openInstance(instance) },
                        onMoreClicked = { onMoreClicked(instance) })
                )
            }
            _submittedReportListFormInstance.postValue(resultList)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }


    override fun listOutbox() {
        _progress.postValue(true)
        getReportsUseCase.setEntityStatus(EntityStatus.FINALIZED)
        getReportsUseCase.execute(onSuccess = { result ->
            val resultList = mutableListOf<ViewEntityTemplateItem>()
            result.map { instance ->
                resultList.add(
                    instance.toViewEntityInstanceItem(onOpenClicked = { openInstance(instance) },
                        onMoreClicked = { onMoreClicked(instance) })
                )
            }
            _outboxReportListFormInstance.postValue(resultList)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    override fun listDraftsOutboxAndSubmitted() {
        _progress.postValue(true)

        // Initialize counters for lengths
        var draftLength: Int = 0
        var outboxLength: Int = 0
        var submittedLength: Int = 0

        // Execute the Draft report retrieval
        getReportsUseCase.setEntityStatus(EntityStatus.DRAFT)
        getReportsUseCase.execute(
            onSuccess = { draftResult ->
                draftLength = draftResult.size // Get the length of drafts

                // Now execute the Outbox report retrieval
                getReportsUseCase.setEntityStatus(EntityStatus.FINALIZED)
                getReportsUseCase.execute(
                    onSuccess = { outboxResult ->
                        outboxLength = outboxResult.size // Get the length of outbox

                        // Now execute the Submitted report retrieval
                        getReportsUseCase.setEntityStatus(EntityStatus.SUBMITTED)
                        getReportsUseCase.execute(
                            onSuccess = { submittedResult ->
                                submittedLength = submittedResult.size // Get the length of submitted

                                // Post the combined lengths to LiveData
                                _reportCounts.postValue(ReportCounts(outboxLength, submittedLength,draftLength))
                            },
                            onError = {
                                _error.postValue(it)
                            },
                            onFinished = {
                                _progress.postValue(false)
                            }
                        )
                    },
                    onError = {
                        _error.postValue(it)
                    },
                    onFinished = {
                        // Handle progress here if needed
                    }
                )
            },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                // Handle progress here if needed
            }
        )
    }

    override fun listDrafts() {
        _progress.postValue(true)
        getReportsUseCase.setEntityStatus(EntityStatus.DRAFT)

        getReportsUseCase.execute(onSuccess = { result ->
            val resultList = mutableListOf<ViewEntityTemplateItem>()

            result.map { instance ->
                resultList.add(
                    instance.toViewEntityInstanceItem(onOpenClicked = { openInstance(instance) },
                        onMoreClicked = { onMoreClicked(instance) })
                )
            }
            _draftListReportFormInstance.postValue(resultList)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    override fun submitReport(instance: ReportInstance, backButtonPressed: Boolean) {
        getReportsServersUseCase.execute(onSuccess = { result ->
            if (backButtonPressed && instance.status != EntityStatus.SUBMITTED) {
                updateInstanceStatus(instance, EntityStatus.SUBMISSION_IN_PROGRESS)
            }

            if (!statusProvider.isOnline()) {
                updateInstanceStatus(instance, EntityStatus.SUBMISSION_PENDING)
            }

            val serverInfo = result.first();
            // Create OwnCloudClient with the server credentials
            val ownCloudClient = OwnCloudClientFactory.createOwnCloudClient(
                Uri.parse(serverInfo.url), // Server URL
                context, // Application context
                true // Use https (or false if http)
            ).apply {
                credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                    serverInfo.username,
                    serverInfo.password
                )
                userId = serverInfo.userId
            }

            if (instance.reportApiId.isEmpty()) {
                createFolderAndSubmitFiles(instance, result.first(), ownCloudClient)
            } else if (instance.status != EntityStatus.SUBMITTED) {
                submitFiles(instance, instance.reportApiId, ownCloudClient)
            }
        }, onError = { error ->
            _error.postValue(error)
        }, onFinished = {
            _progress.postValue(false)
        })

    }

    private fun createFolderAndSubmitFiles(
        instance: ReportInstance,
        server: NextCloudServer,
        ownCloudClient: OwnCloudClient
    ) {
        disposables.add(
            nextCloudRepository.uploadDescription(
                ownCloudClient,
                server.folderName,
                instance.title,
                instance.description
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ folderId ->
                    instance.reportApiId = folderId
                    updateInstanceStatus(instance, EntityStatus.SUBMISSION_IN_PROGRESS)
                    submitFiles(instance, folderId, ownCloudClient)
                }, { error ->
                    handleSubmissionError(instance, error)
                })
        )
    }

    private fun submitFiles(
        instance: ReportInstance,
        folderPath: String,
        ownCloudClient: OwnCloudClient
    ) {
        if (instance.widgetMediaFiles.isEmpty()) {
            handleInstanceStatus(instance, EntityStatus.SUBMITTED)
            return
        }

        disposables.add(
            Flowable.fromIterable(instance.widgetMediaFiles)
                .flatMap { file ->
                    // Fetch the file stream first and create a temp file
                    Single.fromCallable {
                        val inputStream = MediaFileHandler.getStream(file)
                        val tempFile =
                            inputStream?.let { createTempFile(file, it) }  // Create the temp file
                        tempFile // Return the temp file
                    }
                        .toFlowable()  // Convert Single to Flowable so it works with flatMap
                        .flatMap { tempFile ->
                            // Proceed with upload using the temporary file
                            nextCloudRepository.uploadFileWithProgress(
                                ownCloudClient,
                                folderPath,
                                file,
                                tempFile // Pass the temp file to the upload method
                            )
                                .doOnTerminate {
                                    // Delete the temporary file after upload completes
                                    tempFile.delete()
                                }
                                .doOnEach {
                                    if (instance.status != EntityStatus.SUBMITTED) {
                                        instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
                                    }
                                }
                        }
                }
                .doOnTerminate { handleInstanceOnTerminate(instance) }
                .doOnCancel { handleInstanceStatus(instance, EntityStatus.PAUSED) }
                .doOnError {
                    handleInstanceStatus(instance, EntityStatus.SUBMISSION_ERROR)
                }
                .doOnNext { progressInfo: UploadProgressInfo ->
                    updateFileStatus(instance, progressInfo) // Ensure this block is efficient
                }
                .doAfterNext { progressInfo ->
                    _reportProcess.postValue(Pair(progressInfo, instance)) // Post progress quickly
                }
                .observeOn(AndroidSchedulers.mainThread())  // Move results to main thread
                .subscribeOn(Schedulers.io())  // Keep the upload process on IO thread
                .subscribe()
        )
    }

    private fun createTempFile(file: FormMediaFile, inputStream: InputStream): File {
        // Create a temp file to store the content
        val tempFile = File.createTempFile(file.name, ".tmp") // You can also specify your own file extension
        tempFile.deleteOnExit() // Ensure temp file is deleted when the JVM exits

        // Copy content from the input stream to the temp file
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        return tempFile
    }

    private fun handleInstanceStatus(
        instance: ReportInstance, status: EntityStatus
    ) {
        instance.status = status
        nextCloudDataSource.saveInstance(instance).subscribeOn(Schedulers.io())
            .subscribe({
            }, { throwable ->
                throwable.printStackTrace()
            })
        _instanceProgress.postValue(instance)
    }

    private fun updateFileStatus(instance: ReportInstance, progressInfo: UploadProgressInfo) {
        val file = instance.widgetMediaFiles.first { it.id == progressInfo.fileId }
        file.apply {
            status =
                if (progressInfo.status == UploadProgressInfo.Status.FINISHED) FormMediaFileStatus.SUBMITTED else FormMediaFileStatus.NOT_SUBMITTED
            uploadedSize = progressInfo.current
        }
        instance.widgetMediaFiles.first { it.id == progressInfo.fileId }.apply {
            status = file.status
            uploadedSize = file.uploadedSize
        }
        nextCloudDataSource.saveInstance(instance).subscribe()
    }

    private fun handleInstanceOnTerminate(instance: ReportInstance) {
        if (!instance.widgetMediaFiles.any { it.status == FormMediaFileStatus.SUBMITTED }) {
            handleInstanceStatus(instance, EntityStatus.SUBMISSION_PENDING)
        } else {
            handleInstanceStatus(instance, EntityStatus.SUBMITTED)
        }
    }

    override fun saveSubmitted(reportInstance: ReportInstance) {
        _progress.postValue(true)
        saveReportFormInstanceUseCase.setReportFormInstance(reportInstance)
        saveReportFormInstanceUseCase.execute(onSuccess = { result ->
            _reportInstance.postValue(result)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }


    override fun saveOutbox(reportInstance: ReportInstance) {
        _progress.postValue(true)
        saveReportFormInstanceUseCase.setReportFormInstance(reportInstance)
        saveReportFormInstanceUseCase.execute(onSuccess = { result ->
            _reportInstance.postValue(result)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    override fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean) {
        _progress.postValue(true)
        saveReportFormInstanceUseCase.setReportFormInstance(reportInstance)
        saveReportFormInstanceUseCase.execute(onSuccess = { result ->
            _reportInstance.postValue(result)
            _exitAfterSave.postValue(exitAfterSave)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    override fun listServers() {
        _progress.postValue(true)
        getReportsServersUseCase.execute(onSuccess = { result ->
            _serversList.postValue(result)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    private fun handleSubmissionError(instance: ReportInstance, error: Throwable) {
        _error.postValue(error)
        updateInstanceStatus(instance, EntityStatus.SUBMISSION_ERROR)
    }

    private fun updateInstanceStatus(instance: ReportInstance, status: EntityStatus) {
        instance.status = status
        disposables.add(
            nextCloudDataSource.saveInstance(instance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        )
    }

    override fun clearDisposable() {
        disposables.clear()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }

}


