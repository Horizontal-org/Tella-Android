package rs.readahead.washington.mobile.views.fragment.googledrive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.GoogleDriveDataSource
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.googledrive.GoogleDriveRepository
import rs.readahead.washington.mobile.domain.usecases.googledrive.DeleteReportUseCase
import rs.readahead.washington.mobile.domain.usecases.googledrive.GetReportBundleUseCase
import rs.readahead.washington.mobile.domain.usecases.googledrive.GetReportsServersUseCase
import rs.readahead.washington.mobile.domain.usecases.googledrive.GetReportsUseCase
import rs.readahead.washington.mobile.domain.usecases.googledrive.SaveReportFormInstanceUseCase
import rs.readahead.washington.mobile.util.StatusProvider
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
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
    val instanceProgress: LiveData<ReportInstance> get() = _instanceProgress


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

    private fun openInstance(reportInstance: ReportInstance) {
        getReportBundle(reportInstance)
    }

    private fun onMoreClicked(reportInstance: ReportInstance) {
        _onMoreClickedFormInstance.postValue(reportInstance)
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
            disposables.add(googleDriveDataSource.getReportMediaFiles(result.instance)
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

    override fun submitReport(instance: ReportInstance, backButtonPressed: Boolean) {
        getReportsServersUseCase.execute(onSuccess = { result ->
            if (backButtonPressed) {
                if (instance.status != EntityStatus.SUBMITTED) {
                    instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
                    disposables.add(
                        googleDriveDataSource.saveInstance(instance)
                            .subscribeOn(Schedulers.io()) // Run on background thread
                            .observeOn(AndroidSchedulers.mainThread()) // Observe result on main thread
                            .subscribe()
                    )
                }
            }

            if (!statusProvider.isOnline()) {
                instance.status = EntityStatus.SUBMISSION_PENDING
                disposables.add(
                    googleDriveDataSource.saveInstance(instance).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe()
                )
            }
            //TODO create an attribute for the server


            if (instance.reportApiId.isEmpty()) {
                disposables.add(
                    googleDriveRepository.createFolder(
                        googleDriveServer = result.first(),
                        result.first().folderId,
                        instance.title,
                        instance.description
                    ).subscribeOn(Schedulers.io()) // Ensure network call runs on background thread
                        .observeOn(AndroidSchedulers.mainThread()) // Observe result on main thread
                        .subscribe({ folderId ->
                            instance.reportApiId = folderId
                            instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
                            disposables.add(
                                googleDriveDataSource.saveInstance(instance)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread()).subscribe()
                            )
                            submitFiles(instance, result.first(), folderId)

                        }, { error ->
                            _error.postValue(error)
                            instance.status = EntityStatus.SUBMISSION_ERROR
                            disposables.add(
                                googleDriveDataSource.saveInstance(instance)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread()).subscribe()
                            )
                        })
                )
            } else {
                //TODO send the google drive if we already created a folder
                submitFiles(instance, result.first(), instance.reportApiId)
            }
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }


    private fun submitFiles(
        instance: ReportInstance, server: GoogleDriveServer, reportApiId: String
    ) {
        if (instance.widgetMediaFiles.isEmpty()) {
            handleInstanceStatus(instance, EntityStatus.SUBMITTED)
            return
        }

        disposables.add(
            Flowable.fromIterable(instance.widgetMediaFiles).flatMap { file ->
                googleDriveRepository.uploadFilesWithProgress(reportApiId, server.username, file)
            }.doOnEach {
                if (instance.status != EntityStatus.SUBMITTED) {
                    instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
                }
            }.doOnTerminate { handleInstanceOnTerminate(instance) }
                .doOnCancel { handleInstanceStatus(instance, EntityStatus.PAUSED) }.doOnError {
                    handleInstanceStatus(
                        instance, EntityStatus.SUBMISSION_ERROR
                    )
                }.doOnNext { progressInfo: UploadProgressInfo ->
                    updateFileStatus(instance, progressInfo)
                }.doAfterNext { progressInfo ->
                    _reportProcess.postValue(Pair(progressInfo, instance))
                }.subscribeOn(Schedulers.io()) // Non-blocking operation
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
        googleDriveDataSource.saveInstance(instance).subscribe()
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
        //  googleDriveRepository.getDisposable().clear()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }
}

