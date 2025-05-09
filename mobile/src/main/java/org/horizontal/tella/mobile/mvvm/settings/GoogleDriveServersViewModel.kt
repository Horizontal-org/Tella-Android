package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer
import dagger.hilt.android.lifecycle.HiltViewModel
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.mvvm.base.BaseSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class GoogleDriveServersViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource
) : BaseSettingsViewModel() {

    private val _googleDriveServers = SingleLiveEvent<List<GoogleDriveServer>>()
    val googleDriveServers: LiveData<List<GoogleDriveServer>> = _googleDriveServers

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
                        handleError(throwable, R.string.settings_docu_toast_fail_create_server)
                    }
                )
        )
    }

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
                        handleError(throwable, R.string.settings_docu_toast_fail_delete_server)
                    }
                )
        )
    }

}