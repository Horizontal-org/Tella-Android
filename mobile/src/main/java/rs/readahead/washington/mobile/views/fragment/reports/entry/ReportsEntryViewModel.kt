package rs.readahead.washington.mobile.views.fragment.reports.entry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.usecases.server.GetReportsServersUseCase
import javax.inject.Inject


@HiltViewModel
class ReportsEntryViewModel @Inject constructor(private val getReportsServersUseCase: GetReportsServersUseCase) :
    ViewModel() {

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val _serversList = MutableLiveData<List<TellaReportServer>>()
    val serversList: LiveData<List<TellaReportServer>> get() = _serversList
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error

    init {
        listServers()
    }

    private fun listServers() {
        _progress.postValue(true)
        getReportsServersUseCase.execute(
            onSuccess = { result ->
                _serversList.postValue(result)
            },
            onError = {
                _error.postValue(it)
            },
            onFinished = {
                _progress.postValue(false)
            }
        )
    }

    private fun saveDraft(){

    }
}

