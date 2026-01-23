package org.horizontal.tella.mobile.views.fragment.googledrive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.GoogleDriveDataSource
import org.horizontal.tella.mobile.data.googledrive.GoogleDriveRepository
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.usecases.googledrive.DeleteReportUseCase
import org.horizontal.tella.mobile.domain.usecases.googledrive.GetReportBundleUseCase
import org.horizontal.tella.mobile.domain.usecases.googledrive.GetReportsServersUseCase
import org.horizontal.tella.mobile.domain.usecases.googledrive.GetReportsUseCase
import org.horizontal.tella.mobile.domain.usecases.googledrive.SaveReportFormInstanceUseCase
import org.horizontal.tella.mobile.util.StatusProvider
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportCounts
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import org.horizontal.tella.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportsUseCase: GetReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase,
    private val googleDriveRepository: GoogleDriveRepository,
    private val googleDriveDataSource: GoogleDriveDataSource,
    private val statusProvider: StatusProvider
) : BaseReportsViewModel() {

    protected val _reportProcess = MutableLiveData<Pair<UploadProgressInfo, ReportInstance>>()
    val reportProcess: LiveData<Pair<UploadProgressInfo, ReportInstance>> get() = _reportProcess

    protected val _instanceProgress = MutableLiveData<ReportInstance>()
    val instanceProgress: MutableLiveData<ReportInstance> get() = _instanceProgress


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
        var draftLength: Int
        var outboxLength: Int
        var submittedLength: Int

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
                                submittedLength =
                                    submittedResult.size // Get the length of submitted

                                // Post the combined lengths to LiveData
                                _reportCounts.postValue(
                                    ReportCounts(
                                        outboxLength,
                                        submittedLength,
                                        draftLength
                                    )
                                )
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

        getReportBundleUseCase.execute(
            onSuccess = { result ->
                disposables.add(
                    googleDriveDataSource.getReportMediaFiles(result.instance)
                        .flatMap { files ->  // files: List<FormMediaFile>
                            MyApplication.keyRxVault.getRxVault()
                                .firstOrError()
                                .flatMap { rxVault ->  // rxVault: RxVault
                                    rxVault.get(result.fileIds)
                                        .map { vaultFiles ->  // vaultFiles: List<VaultFile>?
                                            Triple(result.instance, files, vaultFiles)
                                        }
                                }
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ (instance, files, vaultFiles) ->
                            val filesResult = processMediaFiles(files, vaultFiles)
                            instance.widgetMediaFiles = filesResult
                            _reportInstance.postValue(instance)
                        }, { throwable ->
                            Timber.d(throwable)
                            FirebaseCrashlytics.getInstance().recordException(throwable)
                        })
                )
            },
            onError = { _error.postValue(it) },
            onFinished = { _progress.postValue(false) }
        )
    }

    private fun processMediaFiles(
        files: List<FormMediaFile>,
        vaultFiles: List<VaultFile>
    ): ArrayList<FormMediaFile> {
        return files.mapNotNull { formMediaFile ->
            vaultFiles.firstOrNull { it.id == formMediaFile.id }?.let { vaultFile ->
                FormMediaFile.fromMediaFile(vaultFile).apply {
                    status = formMediaFile.status
                    uploadedSize = formMediaFile.uploadedSize
                }
            }
        }.toCollection(ArrayList())
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
            googleDriveDataSource.saveInstance(instance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        )
    }

    private fun createFolderAndSubmitFiles(instance: ReportInstance, server: GoogleDriveServer) {
        disposables.add(
            googleDriveRepository.createFolder(
                server,
                server.folderId,
                instance.title,
                instance.description
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

    private fun handleSubmissionError(instance: ReportInstance, error: Throwable) {
        _error.postValue(error)
        updateInstanceStatus(instance, EntityStatus.SUBMISSION_ERROR)
    }


    private fun submitFiles(
        instance: ReportInstance, server: GoogleDriveServer, reportApiId: String
    ) {
        if (instance.widgetMediaFiles.isEmpty()) {
            handleInstanceStatus(instance, EntityStatus.SUBMITTED)
            return
        }
        disposables.add(
            Flowable.fromIterable(instance.widgetMediaFiles)
                .flatMap { file ->
                    googleDriveRepository.uploadFileWithProgress(
                        reportApiId,
                        server.username,
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
        googleDriveDataSource.saveInstance(instance).subscribe()
    }

    private fun handleInstanceStatus(
        instance: ReportInstance, status: EntityStatus
    ) {
        instance.status = status
        googleDriveDataSource.saveInstance(instance).subscribeOn(Schedulers.io())
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


    override fun clearDisposable() {
        disposables.clear()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }
}

