package rs.readahead.washington.mobile.views.fragment.reports.entry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.usecases.reports.*
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import javax.inject.Inject


@HiltViewModel
class ReportsEntryViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportsUseCase: GetReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase
) :
    ViewModel() {

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
    private val _onMoreClickedFormInstance = MutableLiveData<ReportFormInstance>()
    val onMoreClickedFormInstance: LiveData<ReportFormInstance> get() = _onMoreClickedFormInstance
    private val _onOpenClickedFormInstance = MutableLiveData<ReportFormInstance>()
    val onOpenClickedFormInstance: LiveData<ReportFormInstance> get() = _onOpenClickedFormInstance
    private val _instanceDeleted = MutableLiveData<Boolean>()
    val instanceDeleted: LiveData<Boolean> get() = _instanceDeleted
    private val _draftReportInstance = MutableLiveData<ReportFormInstance>()
    val draftReportInstance: LiveData<ReportFormInstance> get() = _draftReportInstance

    fun listServers() {
        _progress.postValue(true)
        getReportsServersUseCase.execute(
            onSuccess = { result ->
                _serversList.postValue(result)
            },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                _progress.postValue(false)
            }
        )
    }

    fun saveDraft(reportFormInstance: ReportFormInstance) {
        _progress.postValue(true)
        saveReportFormInstanceUseCase.setReportFormInstance(reportFormInstance)
        saveReportFormInstanceUseCase.execute(
            onSuccess = { result ->
                _draftReportFormInstance.postValue(result)
            },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                _progress.postValue(false)
            }
        )
    }

    fun saveOutbox(reportFormInstance: ReportFormInstance) {
        _progress.postValue(true)
        saveReportFormInstanceUseCase.setReportFormInstance(reportFormInstance)
        saveReportFormInstanceUseCase.execute(
            onSuccess = { result ->
                _outboxReportFormInstance.postValue(result)
            },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                _progress.postValue(false)
            }
        )
    }

    fun listDrafts() {
        _progress.postValue(true)
        getReportsUseCase.setEntityStatus(EntityStatus.DRAFT)
        getReportsUseCase.execute(
            onSuccess = { result ->
                val resultList = mutableListOf<ViewEntityTemplateItem>()

                result.map { instance ->
                    resultList.add(
                        instance.toViewEntityInstanceItem(
                            onOpenClicked = { openInstance(instance) },
                            onMoreClicked = { onMoreClicked(instance) })
                    )

                }
                _draftListReportFormInstance.postValue(resultList)
            },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                _progress.postValue(false)
            }
        )

    }

    private fun listOutbox() {

    }

    private fun listSubmitted() {

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
        server: TellaReportServer
    ): ReportFormInstance {
        return ReportFormInstance(
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
        server: TellaReportServer
    ): ReportFormInstance {
        return ReportFormInstance(
            title = title,
            description = description,
            status = EntityStatus.FINALIZED,
            widgetMediaFiles = files ?: emptyList(),
            formPartStatus = FormMediaFileStatus.NOT_SUBMITTED,
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

    fun mediaFilesToVaultFiles(files: List<FormMediaFile>): ArrayList<VaultFile> {
        val vaultFiles = ArrayList<VaultFile>()
        files.map { mediaFile ->
            vaultFiles.add(mediaFile.getVaultFile())
        }
        return vaultFiles
    }

    fun putVaultFilesInForm(vaultFileList: String) : ArrayList<VaultFile>{
        val vaultFormfiles: ArrayList<VaultFile> = arrayListOf()
        val files = Gson().fromJson<ArrayList<String>>(
            vaultFileList as String?,
            object : TypeToken<List<String?>?>() {}.type
        )
        for (i in 0 until files.size) {
            if (files.isNotEmpty() && files[i].isNotEmpty()) {
                val vaultFile = MyApplication.rxVault[files[i]]
                    .subscribeOn(Schedulers.io())
                    .blockingGet()
                val file = FormMediaFile.fromMediaFile(vaultFile)
                vaultFormfiles.add(file)
            }
        }
        return vaultFormfiles
    }

    fun deleteReport(instance: ReportFormInstance) {
        _progress.postValue(true)
        deleteReportUseCase.setId(instance.id)

        deleteReportUseCase.execute(
            onSuccess = {
                _instanceDeleted.postValue(true)
            },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                _progress.postValue(false)
            }
        )
    }

    fun getDraftBundle(instance: ReportFormInstance) {
        _progress.postValue(true)
        getReportBundleUseCase.setId(instance.id)

        getReportBundleUseCase.execute(
            onSuccess = { result ->
                var resultInstance: ReportFormInstance = result.instance
                val widgetMediaFiles = mutableListOf<FormMediaFile>()

                val fileList:List<VaultFile> = MyApplication.rxVault.get(result.fileIds).blockingGet()
                for (file in fileList) {
                    widgetMediaFiles.add(FormMediaFile.fromMediaFile(file))
                }
                resultInstance.widgetMediaFiles = widgetMediaFiles
                _draftReportInstance.postValue(resultInstance)
            },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                _progress.postValue(false)
            }
        )
    }

}

