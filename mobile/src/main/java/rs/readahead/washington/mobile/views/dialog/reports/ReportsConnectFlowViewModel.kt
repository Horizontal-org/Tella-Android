package rs.readahead.washington.mobile.views.dialog.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.usecases.reports.CheckReportsServerUseCase
import rs.readahead.washington.mobile.domain.usecases.reports.GetReportsServersUseCase
import javax.inject.Inject

@HiltViewModel
class ReportsConnectFlowViewModel @Inject constructor(
    private val useCase: CheckReportsServerUseCase,
    private val getReportsServersUseCase: GetReportsServersUseCase,
) :
    ViewModel() {

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error
    private val _authenticationSuccess = SingleLiveEvent<TellaReportServer>()
    val authenticationSuccess: LiveData<TellaReportServer> get() = _authenticationSuccess
    private val _serverAlreadyExist = MutableLiveData<Boolean>()
    val serverAlreadyExist: LiveData<Boolean> get() = _serverAlreadyExist

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