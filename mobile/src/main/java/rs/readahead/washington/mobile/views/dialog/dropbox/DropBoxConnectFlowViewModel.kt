package rs.readahead.washington.mobile.views.dialog.dropbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.database.DropBoxDataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.dropbox.DropBoxServer
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.usecases.dropbox.UpdateDropBoxUseCase
import javax.inject.Inject

@HiltViewModel
class DropBoxConnectFlowViewModel @Inject constructor(
    private val updateDropBoxUseCase: UpdateDropBoxUseCase,
) : ViewModel() {
    private val disposables = CompositeDisposable()
    protected val _refreshServerSuccess = MutableLiveData<DropBoxServer>()
    val refreshServerSuccess: LiveData<DropBoxServer> get() = _refreshServerSuccess
    protected val _refreshServerError = MutableLiveData<String>()
    val refreshServerError: LiveData<String> get() = _refreshServerError

    fun refreshDropBoxServer(server: DropBoxServer) {
        updateDropBoxUseCase.setDropBox(server)
        updateDropBoxUseCase.execute(onSuccess = {
            _refreshServerSuccess.postValue(server)
        }, onError = {
            _refreshServerError.postValue(it.message)
        }, onFinished = {

        })
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}