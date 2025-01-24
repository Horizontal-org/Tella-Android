package org.horizontal.tella.mobile.mvvm.odk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.data.repository.OpenRosaRepository
import org.horizontal.tella.mobile.domain.entity.IErrorBundle
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer
import org.horizontal.tella.mobile.domain.entity.collect.ListFormResult
import org.horizontal.tella.mobile.domain.repository.IOpenRosaRepository
import javax.inject.Inject


@HiltViewModel
class CheckOdkServerViewModel @Inject constructor(
    private val mApplication: Application,
) : AndroidViewModel(mApplication) {
    private val odkRepository: IOpenRosaRepository = OpenRosaRepository()
    private val disposables = CompositeDisposable()

    // LiveData for server check states
    private val _loading = SingleLiveEvent<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _serverCheckSuccess = SingleLiveEvent<CollectServer>()
    val serverCheckSuccess: LiveData<CollectServer> = _serverCheckSuccess

    private val _serverCheckFailure = SingleLiveEvent<IErrorBundle>()
    val serverCheckFailure: LiveData<IErrorBundle> = _serverCheckFailure

    private val _serverCheckError = SingleLiveEvent<Throwable>()
    val serverCheckError: LiveData<Throwable> = _serverCheckError

    private val _noConnectionAvailable = SingleLiveEvent<Boolean>()
    val noConnectionAvailable: LiveData<Boolean> = _noConnectionAvailable


    private var saveAnyway = false

    // Method to check server status
    fun checkServer(server: CollectServer, connectionRequired: Boolean) {
        // Check if the device is connected to the internet
        if (!MyApplication.isConnectedToInternet(mApplication)) {
            // No connection, handle accordingly
            if (saveAnyway && !connectionRequired) {
                // If saving anyway and no connection is required, mark server as unchecked and post success
                server.isChecked = false
                _serverCheckSuccess.postValue(server)
            } else {
                // If no connection and connection is required, show error and trigger no connection state
                _noConnectionAvailable.postValue(true)
                setSaveAnyway(true)
            }
            return
        } else {
            // If the device is connected, ensure 'saveAnyway' flag is reset
            if (saveAnyway) {
                setSaveAnyway(false)
            }
        }

        // Clear cache before making the request
        OpenRosaService.clearCache()

        // Show loading state
        _loading.postValue(true)
        // Make the network call
        disposables.add(odkRepository.formList(server)
            .subscribeOn(Schedulers.io())  // Perform network operation on IO thread
            .observeOn(AndroidSchedulers.mainThread())  // Update UI on the main thread
            .doFinally {
                _loading.postValue(false)  // Hide loading after the operation completes
            }
            .subscribe(
                { listFormResult: ListFormResult ->
                    // Handle successful response
                    if (listFormResult.errors.isNotEmpty()) {
                        val errorBundle = listFormResult.errors[0]
                        _serverCheckFailure.postValue(errorBundle)  // Post error bundle to handle failure
                    } else {
                        server.isChecked = true
                        _serverCheckSuccess.postValue(server)  // Post server data on success
                    }
                },
                { throwable: Throwable ->
                    // Log error and post it for error handling
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                    _serverCheckError.postValue(throwable)  // Post error for UI to handle
                })
        )
    }

    // Method to enable or disable "Save Anyway"
    private fun setSaveAnyway(enable: Boolean) {
        saveAnyway = enable
    }

    public override fun onCleared() {
        super.onCleared()
        disposables.dispose() // Clean up disposables
    }
}
