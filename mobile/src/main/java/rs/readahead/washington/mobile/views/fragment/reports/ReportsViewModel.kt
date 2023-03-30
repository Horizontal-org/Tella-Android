package rs.readahead.washington.mobile.views.fragment.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.domain.usecases.reports.*
import rs.readahead.washington.mobile.util.fromJsonToObjectList
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportsUseCase: GetReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase,
    private val reportsRepository: ReportsRepository,
    private val dataSource: DataSource
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val _serversList = MutableLiveData<List<TellaReportServer>>()
    val serversList: LiveData<List<TellaReportServer>> get() = _serversList
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error
    private val _draftListReportFormInstance = MutableLiveData<List<ViewEntityTemplateItem>>()
    val draftListReportFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _draftListReportFormInstance
    private val _outboxReportListFormInstance = MutableLiveData<List<ViewEntityTemplateItem>>()
    val outboxReportListFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _outboxReportListFormInstance
    private val _submittedReportListFormInstance = MutableLiveData<List<ViewEntityTemplateItem>>()
    val submittedReportListFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _submittedReportListFormInstance
    private val _onMoreClickedFormInstance = MutableLiveData<ReportInstance>()
    val onMoreClickedInstance: LiveData<ReportInstance> get() = _onMoreClickedFormInstance
    private val _onOpenClickedFormInstance = MutableLiveData<ReportInstance>()
    val onOpenClickedInstance: LiveData<ReportInstance> get() = _onOpenClickedFormInstance
    private val _instanceDeleted = MutableLiveData<String?>()
    val instanceDeleted: LiveData<String?> get() = _instanceDeleted
    private val _reportInstance = MutableLiveData<ReportInstance>()
    val reportInstance: LiveData<ReportInstance> get() = _reportInstance
    private val _progressInfo = MutableLiveData<Pair<UploadProgressInfo, ReportInstance>>()
    val progressInfo: LiveData<Pair<UploadProgressInfo, ReportInstance>> get() = _progressInfo
    private val _entityStatus = MutableLiveData<ReportInstance>()
    val entityStatus: LiveData<ReportInstance> get() = _entityStatus
    private val _exitAfterSave = MutableLiveData<Boolean>()
    val exitAfterSave: LiveData<Boolean> get() = _exitAfterSave

    val reportProcess = reportsRepository.getReportProgress()
    val instanceProgress = reportsRepository.geInstanceProgress()

    fun listServers() {
        _progress.postValue(true)
        getReportsServersUseCase.execute(onSuccess = { result ->
            _serversList.postValue(result)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean) {
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

    fun saveOutbox(reportInstance: ReportInstance) {
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

    fun saveSubmitted(reportInstance: ReportInstance) {
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

    fun listDrafts() {
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

    fun listOutbox() {
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

    fun listSubmitted() {
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

    fun getDraftFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: TellaReportServer,
        id: Long? = null
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

    fun getFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: TellaReportServer,
        id: Long? = null,
        reportApiId: String = "",
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

    fun getFinalizedFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: TellaReportServer,
        id: Long? = null,
        reportApiId: String = "",
    ): ReportInstance {
        return ReportInstance(
            id = id ?: 0L,
            title = title,
            reportApiId = reportApiId,
            description = description,
            status = EntityStatus.SUBMISSION_PENDING,
            widgetMediaFiles = files ?: emptyList(),
            formPartStatus = FormMediaFileStatus.NOT_SUBMITTED,
            serverId = server.id
        )
    }

    fun vaultFilesToMediaFiles(files: List<VaultFile>): List<FormMediaFile> {
        val vaultFiles = mutableListOf<FormMediaFile>()
        files.map { vaultFile ->
            val mediaFile = FormMediaFile.fromMediaFile(vaultFile)
            mediaFile.status = FormMediaFileStatus.NOT_SUBMITTED
            vaultFiles.add(FormMediaFile.fromMediaFile(vaultFile))
        }
        return vaultFiles
    }

    fun mediaFilesToVaultFiles(files: List<FormMediaFile>?): List<VaultFile> {
        val vaultFiles = ArrayList<VaultFile>()
        files?.map { mediaFile ->
            vaultFiles.add(mediaFile.vaultFile)
        }
        return vaultFiles
    }

    fun putVaultFilesInForm(vaultFileList: String): List<VaultFile> {
        val vaultFormFiles = mutableListOf<VaultFile>()
        val files = vaultFileList.fromJsonToObjectList(String::class.java)
        files?.map { file ->
            //TODO WE NEED TO INJECT RX VAULT WITH DAGGER
            val vaultFile = MyApplication.rxVault[file].subscribeOn(Schedulers.io()).blockingGet()
            val mappedFile = FormMediaFile.fromMediaFile(vaultFile)
            vaultFormFiles.add(mappedFile)
        }
        return vaultFormFiles
    }

    fun deleteReport(instance: ReportInstance) {
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

    fun getReportBundle(instance: ReportInstance) {
        _progress.postValue(true)
        getReportBundleUseCase.setId(instance.id)
        getReportBundleUseCase.execute(onSuccess = { result ->
            val resultInstance = result.instance
            disposables.add(dataSource.getReportMediaFiles(result.instance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ files ->
                    val vaultFiles: MutableList<VaultFile?> =
                        MyApplication.rxVault.get(result.fileIds).blockingGet()
                            ?: return@subscribe
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
                }
            )

        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun submitReport(instance: ReportInstance) {
        getReportsServersUseCase.execute(onSuccess = { servers ->
            val server = servers.first { it.id == instance.serverId }
            reportsRepository.submitReport(
                server,
                instance
            )
        },
            onError = { throwable ->
                if (throwable is NoConnectivityException) {
                    instance.status = EntityStatus.SUBMISSION_PENDING
                } else {
                    instance.status = EntityStatus.SUBMISSION_ERROR
                }
                _entityStatus.postValue(instance)
            },
            onFinished = {
            }
        )

    }

    private fun submitFiles(
        instance: ReportInstance,
        server: TellaReportServer,
        reportApiId: String
    ) {
        disposables.add(
            Flowable.fromIterable(instance.widgetMediaFiles)
                .flatMap { file ->
                    reportsRepository.upload(
                        file,
                        server.url,
                        reportApiId,
                        server.accessToken
                    )
                }.doOnEach {
                    instance.apply {
                        status = EntityStatus.SUBMISSION_IN_PROGRESS
                    }
                }
                .doOnTerminate {
                    instance.status = EntityStatus.SUBMITTED
                    _entityStatus.postValue(instance)
                }.doOnCancel {
                    instance.status = EntityStatus.PAUSED
                    _entityStatus.postValue(instance)
                }.doOnError {
                    instance.status = EntityStatus.SUBMISSION_ERROR
                    _entityStatus.postValue(instance)
                }.doOnNext { progressInfo: UploadProgressInfo ->
                    val file = instance.widgetMediaFiles.first { it.id == progressInfo.fileId }
                    when (progressInfo.status) {
                        UploadProgressInfo.Status.FINISHED -> {
                            file
                                .apply {
                                    status = FormMediaFileStatus.SUBMITTED
                                    uploadedSize = progressInfo.current
                                }
                        }
                        else -> {
                            file
                                .apply {
                                    uploadedSize = progressInfo.current
                                }
                        }
                    }

                    instance.widgetMediaFiles.first { it.id == progressInfo.fileId }.apply {
                        status = file.status
                        uploadedSize = file.uploadedSize
                    }

                }.doAfterNext { progressInfo ->
                    _progressInfo.postValue(Pair(progressInfo, instance))
                }.subscribe()
        )

    }

    fun dispose() {
        disposables.dispose()
    }

    fun clearDisposable() {
        reportsRepository.getDisposable().clear()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }

}

