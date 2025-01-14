package org.horizontal.tella.mobile.views.dialog.dropbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import org.horizontal.tella.mobile.domain.usecases.dropbox.UpdateDropBoxUseCase
import javax.inject.Inject

@HiltViewModel
class DropBoxConnectFlowViewModel @Inject constructor(
    private val updateDropBoxUseCase: UpdateDropBoxUseCase,
) : ViewModel() {

    private val _refreshServerSuccess = MutableLiveData<DropBoxServer>()
    val refreshServerSuccess: LiveData<DropBoxServer> get() = _refreshServerSuccess
    private val _refreshServerError = MutableLiveData<String>()
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
}