package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hzontal.tella_vault.VaultFile
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.IEntityInstance
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus
import org.horizontal.tella.mobile.util.crash.CrashReporterProvider
import org.horizontal.tella.mobile.util.fromJsonToObjectList
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import timber.log.Timber

/**
 * Draft / outbox / submitted behaviour shared by every connection type. [I] is the concrete
 * instance the connection submits: [org.horizontal.tella.mobile.domain.entity.reports.ReportInstance]
 * for Google Drive, Nextcloud and Tella Web, or
 * [org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance] for Uwazi.
 */
abstract class BaseEntityListViewModel<I : IEntityInstance> : ViewModel() {

    protected val disposables = CompositeDisposable()

    protected val _reportCounts = SingleLiveEvent<ReportCounts>()
    val reportCounts: LiveData<ReportCounts> get() = _reportCounts

    protected val _draftListReportFormInstance = SingleLiveEvent<List<ViewEntityTemplateItem>>()
    val draftListReportFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _draftListReportFormInstance

    protected val _outboxReportListFormInstance = SingleLiveEvent<List<ViewEntityTemplateItem>>()
    val outboxReportListFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _outboxReportListFormInstance

    protected val _submittedReportListFormInstance = SingleLiveEvent<List<ViewEntityTemplateItem>>()
    val submittedReportListFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _submittedReportListFormInstance

    protected val _onMoreClickedFormInstance = SingleLiveEvent<I>()
    val onMoreClickedInstance: LiveData<I> get() = _onMoreClickedFormInstance

    protected val _onOpenClickedFormInstance = MutableLiveData<I>()
    val onOpenClickedInstance: LiveData<I> get() = _onOpenClickedFormInstance

    protected val _instanceDeleted = MutableLiveData<String?>()
    val instanceDeleted: LiveData<String?> get() = _instanceDeleted

    protected val _reportInstance = SingleLiveEvent<I>()
    val reportInstance: LiveData<I> get() = _reportInstance

    protected val _entityStatus = SingleLiveEvent<I>()
    protected val _exitAfterSave = SingleLiveEvent<Boolean>()
    val exitAfterSave: LiveData<Boolean> get() = _exitAfterSave

    // Common LiveData for progress/loading states
    protected val _progress = SingleLiveEvent<Boolean>()
    val progress: LiveData<Boolean> get() = _progress

    // Common LiveData for error handling
    protected val _error = SingleLiveEvent<Throwable>()
    val error: LiveData<Throwable> get() = _error

    protected val _serversList = MutableLiveData<List<Server>>()
    val serversList: LiveData<List<Server>> get() = _serversList

    abstract fun clearDisposable()
    abstract fun deleteReport(instance: I)
    abstract fun getReportBundle(instance: I)

    abstract fun listSubmitted()
    abstract fun listOutbox()
    abstract fun listDraftsOutboxAndSubmitted()
    abstract fun listDrafts()
    abstract fun saveSubmitted(reportInstance: I)
    abstract fun saveOutbox(reportInstance: I)
    abstract fun saveDraft(reportInstance: I, exitAfterSave: Boolean)
    abstract fun listServers()
    abstract fun submitReport(instance: I, backButtonPressed: Boolean)

    // Method to handle error posting
    protected fun handleError(error: Throwable) {
        _error.postValue(error)
    }

    protected fun openInstanceAfterUnexpectedFailure(
        instance: I,
        throwable: Throwable,
        logMessage: String
    ) {
        Timber.e(throwable, logMessage)
        CrashReporterProvider.get().run {
            recordException(throwable)
            log(logMessage)
        }
        instance.widgetMediaFiles = emptyList()
        _reportInstance.postValue(instance)
    }

    // Method to handle progress state
    protected fun showProgress() {
        _progress.postValue(true)
    }

    protected fun hideProgress() {
        _progress.postValue(false)
    }

    // Dispose all disposables to prevent memory leaks
    fun dispose() {
        disposables.dispose()
    }

    protected fun openInstance(reportInstance: I) {
        getReportBundle(reportInstance)
    }

    protected fun onMoreClicked(reportInstance: I) {
        _onMoreClickedFormInstance.postValue(reportInstance)
    }

    protected fun mergeReportAttachments(
        formFiles: List<FormMediaFile>?,
        vaultFiles: List<VaultFile>?
    ): List<FormMediaFile> {
        val existing = (vaultFiles ?: emptyList()).filterNotNull().associateBy { it.id }
        return (formFiles ?: emptyList()).mapNotNull { formFile ->
            existing[formFile.id]?.let { vaultFile ->
                FormMediaFile.fromMediaFile(vaultFile).apply {
                    status = formFile.status
                    uploadedSize = formFile.uploadedSize
                }
            }
        }
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

    fun putVaultFilesInForm(vaultFileList: String): Single<List<VaultFile>> {
        return Single.fromCallable {
            vaultFileList.fromJsonToObjectList(String::class.java) ?: emptyList()
        }
            .flatMap { fileIds ->
                MyApplication.keyRxVault.rxVault
                    .firstOrError()
                    .flatMap { rxVault ->
                        Observable.fromIterable(fileIds)
                            .flatMapSingle { fileId ->
                                rxVault[fileId]
                                    .subscribeOn(Schedulers.io())
                                    .onErrorReturn { null } // safe, allows null
                            }
                            .filter { it != null } // filter out nulls
                            .map { it!! } // safe to force unwrap if you're sure it's not null now
                            .toList()
                    }
            }
            .subscribeOn(Schedulers.io())
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }
}
