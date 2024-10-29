package rs.readahead.washington.mobile.views.fragment.nextCloud

import dagger.hilt.android.lifecycle.HiltViewModel
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import javax.inject.Inject

@HiltViewModel
class NextCloudViewModel @Inject constructor() : BaseReportsViewModel() {
    override fun clearDisposable() {
        TODO("Not yet implemented")
    }

    override fun deleteReport(instance: ReportInstance) {
        TODO("Not yet implemented")
    }

    override fun getReportBundle(instance: ReportInstance) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun getDraftFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long?
    ): ReportInstance {
        TODO("Not yet implemented")
    }

    override fun listSubmitted() {
        TODO("Not yet implemented")
    }

    override fun listOutbox() {
        TODO("Not yet implemented")
    }

    override fun listOutboxAndSubmitted() {
        TODO("Not yet implemented")
    }

    override fun listDrafts() {
        TODO("Not yet implemented")
    }

    override fun saveSubmitted(reportInstance: ReportInstance) {
        TODO("Not yet implemented")
    }

    override fun saveOutbox(reportInstance: ReportInstance) {
        TODO("Not yet implemented")
    }

    override fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean) {
        TODO("Not yet implemented")
    }

    override fun listServers() {
        TODO("Not yet implemented")
    }

    override fun submitReport(instance: ReportInstance, backButtonPressed: Boolean) {
        TODO("Not yet implemented")
    }
}


