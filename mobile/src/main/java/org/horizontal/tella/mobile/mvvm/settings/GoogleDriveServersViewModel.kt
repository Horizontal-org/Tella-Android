package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import dagger.hilt.android.lifecycle.HiltViewModel
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class GoogleDriveServersViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val _googleDriveServers = SingleLiveEvent<List<GoogleDriveServer>>()
    val googleDriveServers: LiveData<List<GoogleDriveServer>> = _googleDriveServers

    private val _loading = SingleLiveEvent<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<Int>()
    val error: LiveData<Int> = _error

    private val _createdServer = SingleLiveEvent<GoogleDriveServer>()
    val createdServer: LiveData<GoogleDriveServer> = _createdServer

    private val _removedServer = SingleLiveEvent<GoogleDriveServer>()
    val removedServer: LiveData<GoogleDriveServer> = _removedServer


    fun getGoogleDriveServers(googleDriveId: String) {
        disposables.add(
            keyDataSource.googleDriveDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle { dataSource ->
                    dataSource.listGoogleDriveServers(googleDriveId)
                }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { list ->
                        _googleDriveServers.postValue(list)
                    },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        //_error.postValue(throwable)
                    }
                )
        )
    }

    fun create(server: GoogleDriveServer) {
        disposables.add(
            keyDataSource.googleDriveDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle { dataSource ->
                    dataSource.saveGoogleDriveServer(server)
                }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { googleDriveServer ->
                        _createdServer.postValue(googleDriveServer)
                    },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.settings_docu_toast_fail_create_server)
                    }
                )
        )
    }

    /**
     * Remove a Google Drive server.
     */
    fun remove(server: GoogleDriveServer) {
        disposables.add(
            keyDataSource.googleDriveDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapCompletable { dataSource ->
                    dataSource.removeGoogleDriveServer(server.id)
                }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    {
                        OpenRosaService.clearCache()
                        _removedServer.postValue(server)
                    },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.settings_docu_toast_fail_delete_server)
                    }
                )
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}