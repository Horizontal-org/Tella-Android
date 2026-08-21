package org.horizontal.tella.mobile.views.dialog.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.domain.repository.ITellaUploadServersRepository
import org.horizontal.tella.mobile.domain.usecases.reports.CheckReportsServerUseCase
import org.horizontal.tella.mobile.domain.usecases.reports.GetReportsServersUseCase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@HiltViewModel
class ReportsConnectFlowViewModel @Inject constructor(
    private val useCase: CheckReportsServerUseCase,
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val tellaUploadServersRepository: ITellaUploadServersRepository,
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error
    private val _authenticationSuccess = SingleLiveEvent<TellaReportServer>()
    val authenticationSuccess: LiveData<TellaReportServer> get() = _authenticationSuccess
    private val _serverAlreadyExist = MutableLiveData<Boolean>()
    val serverAlreadyExist: LiveData<Boolean> get() = _serverAlreadyExist
    private val _doesAutoUploadActivated = MutableLiveData<Boolean>()
    val doesAutoUploadActivated: LiveData<Boolean> get() = _doesAutoUploadActivated
    private val _loadedServer = SingleLiveEvent<TellaReportServer>()
    val loadedServer: LiveData<TellaReportServer> get() = _loadedServer

    fun loadServer(serverId: Long) {
        disposables.add(
            tellaUploadServersRepository.getTellaUploadServer(serverId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { server -> _loadedServer.postValue(server) },
                    { _error.postValue(it) }
                )
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun listServers(serverUrl: String) {
        _progress.postValue(true)
        getReportsServersUseCase.execute(onSuccess = { result ->
            if (result.isEmpty()) {
                _serverAlreadyExist.postValue(false)
            } else {
                _serverAlreadyExist.postValue(result.any { server -> server.url + "p" + "/${server.projectSlug}" == serverUrl })
            }
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun listAutoReports() {
        getReportsServersUseCase.execute(onSuccess = { result ->
            if (result.isEmpty()) {
                _doesAutoUploadActivated.postValue(false)
            } else {
                _doesAutoUploadActivated.postValue(result.any { server -> server.isAutoUpload })
            }
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun checkServer(server: TellaReportServer, projectSlug: String) {
        useCase.saveServer(server, projectSlug)
        _progress.postValue(true)
        useCase.execute(
            onSuccess = { result ->
                _authenticationSuccess.postValue(result)
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