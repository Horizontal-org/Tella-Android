package rs.readahead.washington.mobile.views.fragment.dropbox

import dagger.hilt.android.lifecycle.HiltViewModel
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.usecases.dropbox.GetReportsServersUseCase
import rs.readahead.washington.mobile.domain.usecases.dropbox.GetReportsUseCase
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportCounts
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import javax.inject.Inject

@HiltViewModel
class DropBoxViewModel @Inject constructor(
    private val getReportsUseCase : GetReportsUseCase,
    private val getReportsServersUseCase : GetReportsServersUseCase,
) : BaseReportsViewModel() {

    override fun clearDisposable() {
        disposables.clear()
    }

    override fun deleteReport(instance: ReportInstance) {
    }

    override fun getReportBundle(instance: ReportInstance) {
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

    override fun listOutboxAndSubmitted() {
        _progress.postValue(true)

        // Initialize counters for lengths
        var outboxLength: Int
        var submittedLength: Int

        // Execute the Outbox report retrieval
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
                        _reportCounts.postValue(ReportCounts(outboxLength, submittedLength))
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

    override fun saveSubmitted(reportInstance: ReportInstance) {
    }

    override fun saveOutbox(reportInstance: ReportInstance) {
    }

    override fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean) {
    }

    override fun listServers() {
        _progress.postValue(true)
        getReportsServersUseCase.execute(onSuccess = { result ->
            _serversList.postValue(result)
        }, onError = { error ->
            _error.postValue(error)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    override fun submitReport(instance: ReportInstance, backButtonPressed: Boolean) {
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }
}

