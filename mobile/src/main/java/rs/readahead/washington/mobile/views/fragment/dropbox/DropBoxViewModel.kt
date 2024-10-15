package rs.readahead.washington.mobile.views.fragment.dropbox

import dagger.hilt.android.lifecycle.HiltViewModel
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import javax.inject.Inject

@HiltViewModel
class DropBoxViewModel @Inject constructor(
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
    }

    override fun listOutbox() {
    }

    override fun listOutboxAndSubmitted() {
    }

    override fun listDrafts() {
    }

    override fun saveSubmitted(reportInstance: ReportInstance) {
    }

    override fun saveOutbox(reportInstance: ReportInstance) {
    }

    override fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean) {
    }

    override fun listServers() {
    }

    override fun submitReport(instance: ReportInstance, backButtonPressed: Boolean) {
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }
}

