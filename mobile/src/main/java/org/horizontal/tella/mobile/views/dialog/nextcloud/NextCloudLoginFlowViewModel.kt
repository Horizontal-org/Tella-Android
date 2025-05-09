package org.horizontal.tella.mobile.views.dialog.nextcloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import org.horizontal.tella.mobile.domain.usecases.nextcloud.CheckUserCredentialsUseCase
import org.horizontal.tella.mobile.domain.usecases.nextcloud.ValidateNextcloudServerUrlUseCase
import org.horizontal.tella.mobile.domain.usecases.nextcloud.ValidationResult
import org.horizontal.tella.mobile.views.dialog.nextcloud.sslalert.ValidationError
import javax.inject.Inject

@HiltViewModel
class NextCloudLoginFlowViewModel @Inject constructor(
    private val validateServerUrlUseCase: ValidateNextcloudServerUrlUseCase,
    private val checkUserCredentialsUseCase: CheckUserCredentialsUseCase
) : ViewModel() {

    private val _userInfoResult = MutableLiveData<RemoteOperationResult<UserInfo?>>()
    val userInfoResult: LiveData<RemoteOperationResult<UserInfo?>> get() = _userInfoResult

    private val disposables = CompositeDisposable()
    private val _isValidServer = SingleLiveEvent<Boolean>()
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
        progress.postValue(true)
        val disposable = validateServerUrlUseCase(serverUrl)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                when (result) {

                    is ValidationResult.Success -> {
                        _isValidServer.postValue(result.data)
                        progress.postValue(false)
                    }

                    is ValidationResult.Error -> {
                        // _isValidServer.postValue(false)
                        _error.postValue(
                            ValidationError(
                                result.exception.localizedMessage ?: "Unknown error occurred",
                                result.exception
                            )
                        )
                        progress.postValue(false)
                    }
                }
            }, { throwable ->
                progress.postValue(false)
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