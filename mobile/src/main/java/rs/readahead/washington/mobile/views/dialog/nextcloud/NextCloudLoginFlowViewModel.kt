package rs.readahead.washington.mobile.views.dialog.nextcloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import rs.readahead.washington.mobile.domain.usecases.nextcloud.ValidateNextcloudServerUrlUseCase
import javax.inject.Inject

@HiltViewModel
class NextCloudLoginFlowViewModel @Inject constructor(
    private val useCase: ValidateNextcloudServerUrlUseCase
) : ViewModel() {

    private val _isValidServer = MutableLiveData<Boolean>()
    val isValidServer: LiveData<Boolean> get() = _isValidServer

    fun validateServerUrl(serverUrl: String) {
        useCase(serverUrl) { isValid ->
            _isValidServer.postValue(isValid)
        }
    }
}