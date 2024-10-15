package rs.readahead.washington.mobile.views.fragment.dropbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DropBoxDataSource
import rs.readahead.washington.mobile.data.dropbox.DropBoxRepository
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.usecases.dropbox.DeleteReportUseCase
import rs.readahead.washington.mobile.domain.usecases.dropbox.GetReportBundleUseCase
import rs.readahead.washington.mobile.domain.usecases.dropbox.GetReportsServersUseCase
import rs.readahead.washington.mobile.domain.usecases.dropbox.GetReportsUseCase
import rs.readahead.washington.mobile.domain.usecases.dropbox.SaveReportFormInstanceUseCase
import rs.readahead.washington.mobile.util.StatusProvider
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportCounts
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DropBoxViewModel @Inject constructor(
    private val getReportsUseCase: GetReportsUseCase,
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase,
    private val dropBoxDataSource: DropBoxDataSource,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val dropBoxRepository: DropBoxRepository,
    private val statusProvider: StatusProvider,
) : BaseReportsViewModel() {

    protected val _reportProcess = MutableLiveData<Pair<UploadProgressInfo, ReportInstance>>()
    val reportProcess: LiveData<Pair<UploadProgressInfo, ReportInstance>> get() = _reportProcess

    protected val _instanceProgress = MutableLiveData<ReportInstance>()
    val instanceProgress: MutableLiveData<ReportInstance> get() = _instanceProgress


    override fun clearDisposable() {
        disposables.clear()
    }

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
            disposables.add(dropBoxDataSource.getReportMediaFiles(result.instance)
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
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long?
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

    override fun listOutboxAndSubmitted() {
        _progress.postValue(true)

        // Initialize counters for lengths
        var outboxLength: Int
        var submittedLength: Int

        // Execute the Outbox report retrieval
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
                        _reportCounts.postValue(ReportCounts(outboxLength, submittedLength))
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

    override fun listServers() {
        _progress.postValue(true)
        getReportsServersUseCase.execute(onSuccess = { result ->
            _serversList.postValue(result)
        }, onError = { error ->
            _error.postValue(error)
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

            if (instance.reportApiId.isEmpty()) {
                createFolderAndSubmitFiles(instance, result.first())
            } else if (instance.status != EntityStatus.SUBMITTED) {
                submitFiles(instance, result.first(), instance.reportApiId)
            }
        }, onError = { error ->
            _error.postValue(error)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    private fun updateInstanceStatus(instance: ReportInstance, status: EntityStatus) {
        instance.status = status
        disposables.add(
            dropBoxDataSource.saveInstance(instance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        )
    }

    private fun createFolderAndSubmitFiles(instance: ReportInstance, server: DropBoxServer) {
        val dbxClient = createDropboxClient(server)

        disposables.add(
            dropBoxRepository.createDropboxFolder(
                dbxClient,
                instance.title
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ folderId ->
                    instance.reportApiId = folderId
                    updateInstanceStatus(instance, EntityStatus.SUBMISSION_IN_PROGRESS)
                    submitFiles(instance, server, folderId)
                }, { error ->
                    handleSubmissionError(instance, error)
                })
        )
    }
    // Helper function to create DbxClientV2 instance using access token
    private fun createDropboxClient(server: DropBoxServer): DbxClientV2 {
        val accessToken = server.token // Get the access token from the DropBoxServer object
        val config = DbxRequestConfig.newBuilder("dropbox/tella").build() // Create Dropbox request config
        return DbxClientV2(config, accessToken) // Create and return DbxClientV2 instance
    }

    private fun handleSubmissionError(instance: ReportInstance, error: Throwable) {
        _error.postValue(error)
        updateInstanceStatus(instance, EntityStatus.SUBMISSION_ERROR)
    }


    private fun submitFiles(
        instance: ReportInstance, server: DropBoxServer, folderPath: String
    ) {
        if (instance.widgetMediaFiles.isEmpty()) {
            handleInstanceStatus(instance, EntityStatus.SUBMITTED)
            return
        }

        val dbxClient = createDropboxClient(server)

        disposables.add(
            Flowable.fromIterable(instance.widgetMediaFiles)
                .flatMap { file ->
                    dropBoxRepository.uploadFileWithProgress(
                        dbxClient,
                        folderPath,
                        file
                    )
                }
                .doOnEach {
                    if (instance.status != EntityStatus.SUBMITTED) {
                        instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
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
        dropBoxDataSource.saveInstance(instance).subscribe()
    }

    private fun handleInstanceStatus(
        instance: ReportInstance, status: EntityStatus
    ) {
        instance.status = status
        dropBoxDataSource.saveInstance(instance).subscribeOn(Schedulers.io())
            .subscribe({
            }, { throwable ->
                throwable.printStackTrace()
            })
        _instanceProgress.postValue(instance)
    }

    private fun handleInstanceOnTerminate(instance: ReportInstance) {
        if (!instance.widgetMediaFiles.any { it.status == FormMediaFileStatus.SUBMITTED }) {
            handleInstanceStatus(instance, EntityStatus.SUBMISSION_PENDING)
        } else {
            handleInstanceStatus(instance, EntityStatus.SUBMITTED)
        }
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }
}

