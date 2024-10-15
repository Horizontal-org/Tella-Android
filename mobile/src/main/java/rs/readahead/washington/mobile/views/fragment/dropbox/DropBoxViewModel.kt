package rs.readahead.washington.mobile.views.fragment.dropbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.GoogleDriveDataSource
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.Server
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.googledrive.GoogleDriveServer
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.repository.googledrive.GoogleDriveRepository
import rs.readahead.washington.mobile.domain.usecases.googledrive.DeleteReportUseCase
import rs.readahead.washington.mobile.domain.usecases.googledrive.GetReportBundleUseCase
import rs.readahead.washington.mobile.domain.usecases.googledrive.GetReportsServersUseCase
import rs.readahead.washington.mobile.domain.usecases.googledrive.GetReportsUseCase
import rs.readahead.washington.mobile.domain.usecases.googledrive.SaveReportFormInstanceUseCase
import rs.readahead.washington.mobile.util.StatusProvider
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import rs.readahead.washington.mobile.views.fragment.main_connexions.base.ReportCounts
import rs.readahead.washington.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.reports.mappers.toViewEntityInstanceItem
import timber.log.Timber
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
        TODO("Not yet implemented")
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }
}

