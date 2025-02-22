package org.horizontal.tella.mobile.views.fragment.reports

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.exception.NoConnectivityException
import org.horizontal.tella.mobile.domain.repository.reports.ReportsRepository
import org.horizontal.tella.mobile.domain.usecases.reports.DeleteReportUseCase
import org.horizontal.tella.mobile.domain.usecases.reports.GetReportBundleUseCase
import org.horizontal.tella.mobile.domain.usecases.reports.GetReportsServersUseCase
import org.horizontal.tella.mobile.domain.usecases.reports.GetReportsUseCase
import org.horizontal.tella.mobile.domain.usecases.reports.SaveReportFormInstanceUseCase
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportCounts
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import org.horizontal.tella.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
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
) : BaseReportsViewModel() {

    val reportProcess = reportsRepository.getReportProgress()
    val instanceProgress = reportsRepository.geInstanceProgress()

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
        var draftLength: Int = 0
        var outboxLength: Int = 0
        var submittedLength: Int = 0

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
                                submittedLength = submittedResult.size // Get the length of submitted

                                // Post the combined lengths to LiveData
                                _reportCounts.postValue(ReportCounts(outboxLength, submittedLength,draftLength))
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

    override fun submitReport(instance: ReportInstance, backButtonPressed: Boolean) {
        getReportsServersUseCase.execute(onSuccess = { servers ->
            val server = servers.first { it.id == instance.serverId }
            reportsRepository.submitReport(
                server,
                instance,
                backButtonPressed
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

    override fun clearDisposable() {
        reportsRepository.getDisposable().clear()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        reportsRepository.cleanup()
    }
}

