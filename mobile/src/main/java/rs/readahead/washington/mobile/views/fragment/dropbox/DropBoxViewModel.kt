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

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }
}

