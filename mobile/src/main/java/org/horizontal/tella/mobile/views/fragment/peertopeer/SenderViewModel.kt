package org.horizontal.tella.mobile.views.fragment.peertopeer

import dagger.hilt.android.lifecycle.HiltViewModel
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import javax.inject.Inject

@HiltViewModel
class SenderViewModel @Inject constructor(

) : BaseReportsViewModel() {
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

    override fun listDraftsOutboxAndSubmitted() {
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


