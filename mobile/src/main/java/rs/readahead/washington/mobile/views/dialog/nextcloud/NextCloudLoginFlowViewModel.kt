package rs.readahead.washington.mobile.views.dialog.nextcloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.domain.usecases.nextcloud.CheckUserCredentialsUseCase
import rs.readahead.washington.mobile.domain.usecases.nextcloud.ValidateNextcloudServerUrlUseCase
import rs.readahead.washington.mobile.domain.usecases.nextcloud.ValidationResult
import javax.inject.Inject

@HiltViewModel
class NextCloudLoginFlowViewModel @Inject constructor(
    private val validateServerUrlUseCase: ValidateNextcloudServerUrlUseCase,
    private val checkUserCredentialsUseCase: CheckUserCredentialsUseCase
) : ViewModel() {


    private val _userInfoResult = MutableLiveData<RemoteOperationResult<UserInfo?>>()
    val userInfoResult: LiveData<RemoteOperationResult<UserInfo?>> get() = _userInfoResult

    private val disposables = CompositeDisposable()
    private val _isValidServer = MutableLiveData<Boolean>()
    val isValidServer: LiveData<Boolean> = _isValidServer

    private val _error = MutableLiveData<ValidationError>()
    val error: LiveData<ValidationError> = _error

    fun validateServerUrl(serverUrl: String) {
        val disposable = validateServerUrlUseCase(serverUrl)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                when (result) {
                    is ValidationResult.Success -> {
                        _isValidServer.postValue(result.data)

                    }
                    is ValidationResult.Error -> {
                       // _isValidServer.postValue(false)
                        _error.postValue(ValidationError(result.exception.localizedMessage ?: "Unknown error occurred", result.exception))
                    }
                }
            }, { throwable ->
                _isValidServer.postValue(false)
                _error.postValue(ValidationError(throwable.localizedMessage ?: "Unknown error occurred", throwable))
            })

        disposables.add(disposable)
    }

    fun checkUserCredentials(serverUrl: String, username: String, password: String) {
        disposables.add(
            checkUserCredentialsUseCase.execute(serverUrl, username, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    _userInfoResult.postValue(result)
                }, { error ->
                    _userInfoResult.postValue(RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR))
                })
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}