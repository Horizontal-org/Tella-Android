package rs.readahead.washington.mobile.views.fragment.reports.entry

import android.annotation.SuppressLint
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
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.domain.usecases.reports.*
import rs.readahead.washington.mobile.util.fromJsonToObjectList
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ReportsEntryViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportsUseCase: GetReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase,
    private val submitReportUseCase: SubmitReportUseCase,
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
    private val _onMoreClickedFormInstance = MutableLiveData<ReportFormInstance>()
    val onMoreClickedFormInstance: LiveData<ReportFormInstance> get() = _onMoreClickedFormInstance
    private val _onOpenClickedFormInstance = MutableLiveData<ReportFormInstance>()
    val onOpenClickedFormInstance: LiveData<ReportFormInstance> get() = _onOpenClickedFormInstance
    private val _instanceDeleted = MutableLiveData<Boolean>()
    val instanceDeleted: LiveData<Boolean> get() = _instanceDeleted
    private val _reportInstance = MutableLiveData<ReportFormInstance>()
    val reportInstance: LiveData<ReportFormInstance> get() = _reportInstance
    private val _progressInfo = MutableLiveData<Pair<UploadProgressInfo, ReportFormInstance>>()
    val progressInfo: LiveData<Pair<UploadProgressInfo, ReportFormInstance>> get() = _progressInfo
    private val _entityStatus = MutableLiveData<ReportFormInstance>()
    val entityStatus: LiveData<ReportFormInstance> get() = _entityStatus

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

    fun saveDraft(reportFormInstance: ReportFormInstance) {
        _progress.postValue(true)
        saveReportFormInstanceUseCase.setReportFormInstance(reportFormInstance)
        saveReportFormInstanceUseCase.execute(onSuccess = { result ->
            _reportInstance.postValue(result)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun saveOutbox(reportFormInstance: ReportFormInstance) {
        _progress.postValue(true)
        saveReportFormInstanceUseCase.setReportFormInstance(reportFormInstance)
        saveReportFormInstanceUseCase.execute(onSuccess = { result ->
            _reportInstance.postValue(result)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun saveSubmitted(reportFormInstance: ReportFormInstance) {
        _progress.postValue(true)
        saveReportFormInstanceUseCase.setReportFormInstance(reportFormInstance)
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

    private fun openInstance(reportFormInstance: ReportFormInstance) {
        getDraftBundle(reportFormInstance)
    }

    private fun onMoreClicked(reportFormInstance: ReportFormInstance) {
        _onMoreClickedFormInstance.postValue(reportFormInstance)
    }

    fun getDraftFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: TellaReportServer,
        id: Long? = null
    ): ReportFormInstance {
        return ReportFormInstance(
            id = id ?: 0L,
            title = title,
            description = description,
            status = EntityStatus.DRAFT,
            widgetMediaFiles = files ?: emptyList(),
            formPartStatus = FormMediaFileStatus.NOT_SUBMITTED,
            serverId = server.id
        )
    }

    fun getOutboxFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: TellaReportServer,
        id: Long? = null,
        reportApiId: String = "",
    ): ReportFormInstance {
        return ReportFormInstance(
            id = id ?: 0L,
            title = title,
            reportApiId = reportApiId,
            description = description,
            status = EntityStatus.FINALIZED,
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
    ): ReportFormInstance {
        return ReportFormInstance(
            id = id ?: 0L,
            title = title,
            reportApiId = reportApiId,
            description = description,
            status = EntityStatus.FINALIZED,
            widgetMediaFiles = files ?: emptyList(),
            formPartStatus = FormMediaFileStatus.NOT_SUBMITTED,
            serverId = server.id
        )
    }

    fun getSubmittedFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: TellaReportServer,
        id: Long? = null,
        reportApiId: String = "",
    ): ReportFormInstance {
        return ReportFormInstance(
            id = id ?: 0L,
            title = title,
            reportApiId = reportApiId,
            description = description,
            status = EntityStatus.SUBMITTED,
            widgetMediaFiles = files ?: emptyList(),
            formPartStatus = FormMediaFileStatus.SUBMITTED,
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

    fun mediaFilesToVaultFiles(files: List<FormMediaFile>): List<VaultFile> {
        val vaultFiles = ArrayList<VaultFile>()
        files.map { mediaFile ->
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

    fun deleteReport(instance: ReportFormInstance) {
        _progress.postValue(true)
        deleteReportUseCase.setId(instance.id)

        deleteReportUseCase.execute(onSuccess = {
            _instanceDeleted.postValue(true)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun getDraftBundle(instance: ReportFormInstance) {
        _progress.postValue(true)
        getReportBundleUseCase.setId(instance.id)
        getReportBundleUseCase.execute(onSuccess = { result ->
            val resultInstance = result.instance
            disposables.add(dataSource.getReportMediaFiles(result.instance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ files ->
                    val vaultFiles = MyApplication.rxVault.get(result.fileIds).blockingGet()
                    val filesResult = arrayListOf<FormMediaFile>()

                    files.forEach { formMediaFile ->
                        val vaultFile =
                            vaultFiles.first { vaultFile -> formMediaFile.id == vaultFile.id }
                        if (vaultFile != null) {
                            val fileResult = FormMediaFile.fromMediaFile(vaultFile)
                            fileResult.status = formMediaFile.status
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

    @SuppressLint("CheckResult")
    fun submitReport(instance: ReportFormInstance) {
        _progress.postValue(true)
        getReportsServersUseCase.execute(onSuccess = { servers ->
            val server = servers.first { it.id == instance.serverId }
            // if (!server.isActivatedBackgroundUpload) {
            disposables.add(
                reportsRepository.submitReport(
                    server,
                    ReportBodyEntity(instance.title, instance.description)
                )
                    .doOnError {
                        instance.status = EntityStatus.SUBMISSION_ERROR
                        _entityStatus.postValue(instance)
                    }
                    .doOnDispose {
                        instance.status = EntityStatus.PAUSED
                        _entityStatus.postValue(instance)
                    }
                    .subscribe { reportPostResult ->
                        instance.status = EntityStatus.SUBMISSION_PARTIAL_PARTS
                        instance.reportApiId = reportPostResult.id
                        _entityStatus.postValue(instance)
                        Flowable.fromIterable(instance.widgetMediaFiles)
                            .flatMap { file ->
                                reportsRepository.upload(
                                    file,
                                    server.url,
                                    reportPostResult.id,
                                    server.accessToken
                                )
                            }.doOnComplete {
                                instance.status = EntityStatus.SUBMITTED
                                instance.formPartStatus = FormMediaFileStatus.SUBMITTED
                                _entityStatus.postValue(instance)
                            }.doOnCancel {
                                instance.status = EntityStatus.PAUSED
                                _entityStatus.postValue(instance)
                            }
                            .subscribeOn(Schedulers.io(), true)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ progressInfo: UploadProgressInfo ->
                                when (progressInfo.status) {
                                    UploadProgressInfo.Status.ERROR, UploadProgressInfo.Status.UNAUTHORIZED, UploadProgressInfo.Status.UNKNOWN_HOST, UploadProgressInfo.Status.UNKNOWN, UploadProgressInfo.Status.CONFLICT -> {
                                        instance.widgetMediaFiles.first { it.id == progressInfo.fileId }
                                            .apply {
                                                status = FormMediaFileStatus.NOT_SUBMITTED
                                                uploadedSize = progressInfo.current
                                            }
                                        dataSource.saveInstance(instance)
                                    }
                                    UploadProgressInfo.Status.FINISHED, UploadProgressInfo.Status.OK, UploadProgressInfo.Status.STARTED -> {
                                        instance.widgetMediaFiles.first { it.id == progressInfo.fileId }
                                            .apply {
                                                status = FormMediaFileStatus.SUBMITTED
                                                uploadedSize = progressInfo.current
                                            }
                                        _progressInfo.postValue(Pair(progressInfo, instance))
                                    }
                                    else -> {
                                    }
                                }
                            }) { throwable: Throwable? ->
                                instance.status = EntityStatus.SUBMISSION_ERROR
                                _entityStatus.postValue(instance)
                                Timber.d(throwable)
                                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                            }

                    })

        },
            onError = {
                instance.status = EntityStatus.SUBMISSION_ERROR
                dataSource.saveInstance(instance)
            },
            onFinished = {

            }
        )

    }

    private fun updateProgress(instance: ReportFormInstance) {
        when (instance.status) {
            EntityStatus.FINALIZED -> {

            }
        }
    }

    fun dispose() {
        disposables.dispose()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }

}

