package rs.readahead.washington.mobile.views.dialog.reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.usecases.reports.CheckReportsServerUseCase
import javax.inject.Inject

@HiltViewModel
class ReportsConnectFlowViewModel @Inject constructor(private val useCase: CheckReportsServerUseCase) :
    ViewModel() {

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error
    private val _authenticationSuccess = SingleLiveEvent<Boolean>()
    val authenticationSuccess: LiveData<Boolean> get() = _authenticationSuccess

    fun checkServer(server: TellaReportServer) {
        useCase.saveServer(server)
        _progress.postValue(true)
        useCase.execute(
            onSuccess = { result ->
                server.accessToken = result.accessToken
                _authenticationSuccess.postValue(true)
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