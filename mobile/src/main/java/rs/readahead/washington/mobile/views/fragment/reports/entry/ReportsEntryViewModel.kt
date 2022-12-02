package rs.readahead.washington.mobile.views.fragment.reports.entry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.reports.ProjectResult
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.usecases.reports.*
import rs.readahead.washington.mobile.util.fromJsonToObjectList
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import javax.inject.Inject

@HiltViewModel
class ReportsEntryViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportsUseCase: GetReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase,
    private val submitReportUseCase: SubmitReportUseCase,
    private val getReportProjectsUseCase: GetReportProjectsUseCase
) : ViewModel() {

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val _serversList = MutableLiveData<List<TellaReportServer>>()
    val serversList: LiveData<List<TellaReportServer>> get() = _serversList
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error
    private val _draftListReportFormInstance = MutableLiveData<List<ViewEntityTemplateItem>>()
    val draftListReportFormInstance: LiveData<List<ViewEntityTemplateItem>> get() = _draftListReportFormInstance
    private val _draftReportFormInstance = MutableLiveData<ReportFormInstance>()
    val draftReportFormInstance: LiveData<ReportFormInstance> get() = _draftReportFormInstance
    private val _outboxReportFormInstance = MutableLiveData<ReportFormInstance>()
    val outboxReportFormInstance: LiveData<ReportFormInstance> get() = _outboxReportFormInstance
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
    private val _draftReportInstance = MutableLiveData<ReportFormInstance>()
    val draftReportInstance: LiveData<ReportFormInstance> get() = _draftReportInstance
    private val _serverProjectList = MutableLiveData<List<ProjectResult>>()
    val serverProjectList: LiveData<List<ProjectResult>> get() = _serverProjectList

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
            _draftReportFormInstance.postValue(result)
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
            _outboxReportFormInstance.postValue(result)
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
            _outboxReportFormInstance.postValue(result)
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
            //TODO WE NEED TO INJECT RXX VAULT USING DAGGER
            resultInstance.widgetMediaFiles =
                vaultFilesToMediaFiles(MyApplication.rxVault.get(result.fileIds).blockingGet())
            _draftReportInstance.postValue(resultInstance)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun submitReport(
        title: String,
        description: String,
        server: TellaReportServer,
        files: List<FormMediaFile>
    ) {
        _progress.postValue(true)
        submitReportUseCase.setData(
            server = server, reportBodyEntity = ReportBodyEntity(title, description)
        )
        submitReportUseCase.execute(onSuccess = { result ->
            if (files.isEmpty()) {
                saveSubmitted(
                    getSubmittedFormInstance(
                        title = title,
                        description = description,
                        files = files,
                        server = server,
                        reportApiId = result.id
                    )
                )
            } else {
                saveOutbox(
                    reportFormInstance = getOutboxFormInstance(
                        title = title,
                        description = description,
                        files = files,
                        server = server,
                        reportApiId = result.id
                    )
                )
            }

        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun listReportProjects(servers: List<TellaReportServer>) {
        _progress.postValue(true)
        getReportProjectsUseCase.setReportServersList(servers)
        getReportProjectsUseCase.execute(
            onSuccess = { result -> _serverProjectList.pservers = {ArrayList@9178}  size = 1ostValue(result) },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                _progress.postValue(false)
            }
        )
    }

}

