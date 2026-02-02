package org.horizontal.tella.mobile.views.fragment.dropbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dropbox.core.v2.DbxClientV2
import dagger.hilt.android.lifecycle.HiltViewModel
import org.horizontal.tella.mobile.data.database.DropBoxDataSource
import org.horizontal.tella.mobile.data.dropbox.DropBoxRepository
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance
import org.horizontal.tella.mobile.domain.usecases.dropbox.DeleteReportUseCase
import org.horizontal.tella.mobile.domain.usecases.dropbox.GetReportBundleUseCase
import org.horizontal.tella.mobile.domain.usecases.dropbox.GetReportsServersUseCase
import org.horizontal.tella.mobile.domain.usecases.dropbox.GetReportsUseCase
import org.horizontal.tella.mobile.domain.usecases.dropbox.SaveReportFormInstanceUseCase
import org.horizontal.tella.mobile.util.StatusProvider
import org.horizontal.tella.mobile.views.fragment.dropbox.data.RefreshDropBoxServer
import org.horizontal.tella.mobile.views.fragment.main_connexions.base.BaseReportsViewModel
import org.horizontal.tella.mobile.views.fragment.reports.adapter.ViewEntityTemplateItem
import javax.inject.Inject

/**
 * Stub implementation of DropBoxViewModel for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but all operations
 * throw UnsupportedOperationException since Dropbox is not available in F-Droid builds.
 */
@HiltViewModel
class DropBoxViewModel @Inject constructor(
    private val getReportsUseCase: GetReportsUseCase,
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val saveReportFormInstanceUseCase: SaveReportFormInstanceUseCase,
    private val getReportBundleUseCase: GetReportBundleUseCase,
    private val dropBoxDataSource: DropBoxDataSource,
    private val deleteReportUseCase: DeleteReportUseCase,
    private val dropBoxRepository: DropBoxRepository,
    private val statusProvider: StatusProvider,
) : BaseReportsViewModel() {

    protected val _reportProcess = MutableLiveData<Pair<UploadProgressInfo, ReportInstance>>()
    val reportProcess: LiveData<Pair<UploadProgressInfo, ReportInstance>> get() = _reportProcess

    protected val _instanceProgress = MutableLiveData<ReportInstance>()
    val instanceProgress: MutableLiveData<ReportInstance> get() = _instanceProgress

    protected val _tokenExpired = MutableLiveData<RefreshDropBoxServer>()
    val tokenExpired: MutableLiveData<RefreshDropBoxServer> get() = _tokenExpired

    override fun clearDisposable() {
        disposables.clear()
    }

    override fun deleteReport(instance: ReportInstance) {
        _error.postValue(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }

    override fun getReportBundle(instance: ReportInstance) {
        _error.postValue(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
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
        throw UnsupportedOperationException("Dropbox is not available in F-Droid builds")
    }

    override fun getDraftFormInstance(
        title: String,
        description: String,
        files: List<FormMediaFile>?,
        server: Server,
        id: Long?
    ): ReportInstance {
        throw UnsupportedOperationException("Dropbox is not available in F-Droid builds")
    }

    override fun listSubmitted() {
        _submittedReportListFormInstance.postValue(emptyList())
    }

    override fun listOutbox() {
        _outboxReportListFormInstance.postValue(emptyList())
    }

    override fun listDraftsOutboxAndSubmitted() {
        _reportCounts.postValue(org.horizontal.tella.mobile.views.fragment.main_connexions.base.ReportCounts(0, 0, 0))
    }

    override fun listDrafts() {
        _draftListReportFormInstance.postValue(emptyList())
    }

    override fun saveDraft(reportInstance: ReportInstance, exitAfterSave: Boolean) {
        _error.postValue(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }

    override fun saveOutbox(reportInstance: ReportInstance) {
        _error.postValue(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }

    override fun saveSubmitted(reportInstance: ReportInstance) {
        _error.postValue(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }

    override fun listServers() {
        _serversList.postValue(emptyList())
    }

    override fun submitReport(instance: ReportInstance, backButtonPressed: Boolean) {
        _error.postValue(UnsupportedOperationException("Dropbox is not available in F-Droid builds"))
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        disposables.clear()
    }
}




