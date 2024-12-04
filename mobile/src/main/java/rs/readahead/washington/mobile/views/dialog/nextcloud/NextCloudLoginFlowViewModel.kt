package rs.readahead.washington.mobile.views.dialog.nextcloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.domain.entity.nextcloud.NextCloudServer
import rs.readahead.washington.mobile.domain.usecases.nextcloud.CheckUserCredentialsUseCase
import rs.readahead.washington.mobile.domain.usecases.nextcloud.ValidateNextcloudServerUrlUseCase
import rs.readahead.washington.mobile.domain.usecases.nextcloud.ValidationResult
import rs.readahead.washington.mobile.views.dialog.nextcloud.sslalert.ValidationError
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

    val errorUserNamePassword = MutableLiveData<Boolean>()

    val errorFolderNameExist = SingleLiveEvent<String>()

    val successLoginToServer = SingleLiveEvent<NextCloudServer>()
    val successFolderCreation = SingleLiveEvent<NextCloudServer>()

    val progress = MutableLiveData<Boolean>()

    val errorFolderCreation = MutableLiveData<String>()

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
                        _error.postValue(
                            ValidationError(
                                result.exception.localizedMessage ?: "Unknown error occurred",
                                result.exception
                            )
                        )
                    }
                }
            }, { throwable ->
                _isValidServer.postValue(false)
                _error.postValue(
                    ValidationError(
                        throwable.localizedMessage ?: "Unknown error occurred", throwable
                    )
                )
            })

        disposables.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}