package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hzontal.tella_vault.VaultFile
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.util.fromJsonToObjectList
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem

abstract class BaseReportsViewModel : ViewModel() {

    protected val disposables = CompositeDisposable()

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

    protected val _instanceDeleted = SingleLiveEvent<String?>()
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

    protected val _serversList = MutableLiveData<List<TellaReportServer>>()
    val serversList: LiveData<List<TellaReportServer>> get() = _serversList


    // Abstract methods to be implemented in derived ViewModels
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
    abstract fun listDrafts()
    abstract fun saveSubmitted(reportInstance: ReportInstance)
    abstract fun saveOutbox(reportInstance: ReportInstance)
    abstract fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean)
    abstract fun listServers()

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

    override fun onCleared() {
        super.onCleared()
        dispose()
    }
}