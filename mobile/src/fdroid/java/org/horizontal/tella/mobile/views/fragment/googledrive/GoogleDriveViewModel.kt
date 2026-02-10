package org.horizontal.tella.mobile.views.fragment.googledrive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import org.horizontal.tella.mobile.data.database.GoogleDriveDataSource
import org.horizontal.tella.mobile.data.googledrive.GoogleDriveRepository
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.usecases.googledrive.DeleteReportUseCase
import org.horizontal.tella.mobile.domain.usecases.googledrive.GetReportBundleUseCase
import org.horizontal.tella.mobile.domain.usecases.googledrive.GetReportsServersUseCase
import org.horizontal.tella.mobile.domain.usecases.googledrive.GetReportsUseCase
import org.horizontal.tella.mobile.domain.usecases.googledrive.SaveReportFormInstanceUseCase
import org.horizontal.tella.mobile.util.StatusProvider
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportCounts
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import javax.inject.Inject

/**
 * Stub implementation of GoogleDriveViewModel for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but all operations
 * throw UnsupportedOperationException since Google Drive is not available in F-Droid builds.
 */
@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportsUseCase: GetReportsUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase,
    private val googleDriveRepository: GoogleDriveRepository,
    private val googleDriveDataSource: GoogleDriveDataSource,
    private val statusProvider: StatusProvider
) : BaseReportsViewModel() {

    protected val _reportProcess = MutableLiveData<Pair<UploadProgressInfo, ReportInstance>>()
    val reportProcess: LiveData<Pair<UploadProgressInfo, ReportInstance>> get() = _reportProcess

    protected val _instanceProgress = MutableLiveData<ReportInstance>()
    val instanceProgress: MutableLiveData<ReportInstance> get() = _instanceProgress

    private val _showSharedDriveMigrationSheet = MutableLiveData<Unit?>()
    val showSharedDriveMigrationSheet: LiveData<Unit?> get() = _showSharedDriveMigrationSheet

    override fun listServers() {
        _serversList.postValue(emptyList())
    }

    override fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean) {
        _error.postValue(UnsupportedOperationException("Google Drive is not available in F-Droid builds"))
    }

    override fun saveOutbox(reportInstance: ReportInstance) {
        _error.postValue(UnsupportedOperationException("Google Drive is not available in F-Droid builds"))
    }

    override fun saveSubmitted(reportInstance: ReportInstance) {
        _error.postValue(UnsupportedOperationException("Google Drive is not available in F-Droid builds"))
    }

    override fun listDrafts() {
        _draftListReportFormInstance.postValue(emptyList())
    }

    override fun listOutbox() {
        _outboxReportListFormInstance.postValue(emptyList())
    }

    override fun listDraftsOutboxAndSubmitted() {
        _reportCounts.postValue(ReportCounts(0, 0, 0))
    }

    override fun listSubmitted() {
        _submittedReportListFormInstance.postValue(emptyList())
    }

    override fun deleteReport(instance: ReportInstance) {
        _error.postValue(UnsupportedOperationException("Google Drive is not available in F-Droid builds"))
    }

    override fun getReportBundle(instance: ReportInstance) {
        _error.postValue(UnsupportedOperationException("Google Drive is not available in F-Droid builds"))
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
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }

    override fun getDraftFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long?
    ): ReportInstance {
        throw UnsupportedOperationException("Google Drive is not available in F-Droid builds")
    }

    override fun submitReport(instance: ReportInstance, backButtonPressed: Boolean) {
        _error.postValue(UnsupportedOperationException("Google Drive is not available in F-Droid builds"))
    }

    fun consumeSharedDriveMigrationEvent() {
        _showSharedDriveMigrationSheet.postValue(null)
    }

    override fun clearDisposable() {
        disposables.clear()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }
}




