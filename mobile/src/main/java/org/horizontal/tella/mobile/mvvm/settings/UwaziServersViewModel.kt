package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.database.UwaziDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.mvvm.base.BaseSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class UwaziServersViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource
) : BaseSettingsViewModel() {

    private val _listUwaziServers = SingleLiveEvent<List<UWaziUploadServer>>()
    val listUwaziServers: LiveData<List<UWaziUploadServer>> get() = _listUwaziServers

    private val _createdServer = SingleLiveEvent<UWaziUploadServer>()
    val createdServer: LiveData<UWaziUploadServer> get() = _createdServer

    private val _updatedServer = SingleLiveEvent<UWaziUploadServer>()
    val updatedServer: LiveData<UWaziUploadServer> get() = _updatedServer

    private val _removedServer = SingleLiveEvent<UWaziUploadServer>()
    val removedServer: LiveData<UWaziUploadServer> get() = _removedServer

    fun getUwaziServers() {
        disposables.add(
            keyDataSource.uwaziDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle { dataSource: UwaziDataSource -> dataSource.listUwaziServers() }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { servers -> _listUwaziServers.postValue(servers) },
                    { throwable ->
                        handleError(throwable)

                    }
                )
        )
    }

    fun create(server: UWaziUploadServer) {
        disposables.add(
            keyDataSource.uwaziDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle { dataSource: UwaziDataSource -> dataSource.createUWAZIServer(server) }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { createdServer -> _createdServer.postValue(createdServer) },
                    { throwable ->
                        handleError(throwable, R.string.settings_docu_toast_fail_create_server)
                    }
                )
        )
    }

    fun update(server: UWaziUploadServer) {
        disposables.add(
            keyDataSource.uwaziDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle { dataSource: UwaziDataSource -> dataSource.updateUwaziServer(server) }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { updatedServer -> _updatedServer.postValue(updatedServer) },
                    { throwable ->
                        handleError(throwable, R.string.settings_docu_toast_fail_update_server)
                    }
                )
        )
    }

    fun remove(server: UWaziUploadServer) {
        disposables.add(
            keyDataSource.uwaziDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapCompletable { dataSource: UwaziDataSource ->
                    dataSource.removeUwaziServer(
                        server.id
                    )
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
