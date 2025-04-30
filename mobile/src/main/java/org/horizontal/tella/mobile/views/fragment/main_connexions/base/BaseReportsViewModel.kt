package org.horizontal.tella.mobile.views.fragment.main_connexions.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.util.fromJsonToObjectList
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem

abstract class BaseReportsViewModel : ViewModel() {

    protected val disposables = CompositeDisposable()

    protected val _reportCounts = SingleLiveEvent<ReportCounts>()
    val reportCounts: LiveData<ReportCounts> get() = _reportCounts

    protected val _draftListReportFormInstance = SingleLiveEvent<List<ViewEntityTemplateItem>>()
    val draftListReportFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _draftListReportFormInstance

    protected val _outboxReportListFormInstance = SingleLiveEvent<List<ViewEntityTemplateItem>>()
    val outboxReportListFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _outboxReportListFormInstance

    protected val _submittedReportListFormInstance = SingleLiveEvent<List<ViewEntityTemplateItem>>()
    val submittedReportListFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _submittedReportListFormInstance

    protected val _onMoreClickedFormInstance = SingleLiveEvent<ReportInstance>()
    val onMoreClickedInstance: LiveData<ReportInstance> get() = _onMoreClickedFormInstance

    protected val _onOpenClickedFormInstance = MutableLiveData<ReportInstance>()
    val onOpenClickedInstance: LiveData<ReportInstance> get() = _onOpenClickedFormInstance

    protected val _instanceDeleted = MutableLiveData<String?>()
    val instanceDeleted: LiveData<String?> get() = _instanceDeleted

    protected val _reportInstance = SingleLiveEvent<ReportInstance>()
    val reportInstance: LiveData<ReportInstance> get() = _reportInstance

    protected val _entityStatus = SingleLiveEvent<ReportInstance>()
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

    //TODO CHECK FOR LATER AHLEM + WAFA
    // val reportProcess: SingleLiveEvent<Pair<UploadProgressInfo, ReportInstance>>
    // val instanceProgress: SingleLiveEvent<ReportInstance>


    // Abstract methods to be implemented in derived ViewModels
    abstract fun clearDisposable()
    abstract fun deleteReport(instance: ReportInstance)
    abstract fun getReportBundle(instance: ReportInstance)
    abstract fun getFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long? = null,
        reportApiId: String = "",
        status: EntityStatus
    ): ReportInstance

    abstract fun getDraftFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long? = null
    ): ReportInstance

    abstract fun listSubmitted()
    abstract fun listOutbox()
    abstract fun listDraftsOutboxAndSubmitted()
    abstract fun listDrafts()
    abstract fun saveSubmitted(reportInstance: ReportInstance)
    abstract fun saveOutbox(reportInstance: ReportInstance)
    abstract fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean)
    abstract fun listServers()
    abstract fun submitReport(instance: ReportInstance, backButtonPressed: Boolean)

    // Method to handle error posting
    protected fun handleError(error: Throwable) {
        _error.postValue(error)
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

    protected fun openInstance(reportInstance: ReportInstance) {
        getReportBundle(reportInstance)
    }

    protected fun onMoreClicked(reportInstance: ReportInstance) {
        _onMoreClickedFormInstance.postValue(reportInstance)
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
                                    .onErrorReturnItem(null) // Handle potential errors
                            }
                            .filter { true } // Filter out null values
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