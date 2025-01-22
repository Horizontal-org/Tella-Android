package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer
import org.horizontal.tella.mobile.mvvm.base.BaseSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class CollectServersViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource
) : BaseSettingsViewModel() {

    private val _listCollectServers = MutableLiveData<List<CollectServer>>()
    val listCollectServers: LiveData<List<CollectServer>> get() = _listCollectServers

    private val _createdServer = MutableLiveData<CollectServer>()
    val createdServer: LiveData<CollectServer> get() = _createdServer

    private val _updatedServer = MutableLiveData<CollectServer>()
    val updatedServer: LiveData<CollectServer> get() = _updatedServer

    private val _removedServer = MutableLiveData<CollectServer>()
    val removedServer: LiveData<CollectServer> get() = _removedServer


    fun getCollectServers() {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle { dataSource: DataSource -> dataSource.listCollectServers() }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { servers -> _listCollectServers.postValue(servers) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                    }
                )
        )
    }

    fun create(server: CollectServer) {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle { dataSource: DataSource -> dataSource.createCollectServer(server) }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { createdServer -> _createdServer.postValue(createdServer) },
                    { throwable ->
                        handleError(throwable, R.string.settings_docu_toast_fail_create_server)
                    }
                )
        )
    }

    fun update(server: CollectServer) {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle { dataSource: DataSource -> dataSource.updateCollectServer(server) }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { updatedServer ->
                        OpenRosaService.clearCache()
                        _updatedServer.postValue(updatedServer)
                    },
                    { throwable ->
                        handleError(throwable, R.string.settings_docu_toast_fail_update_server)
                    }
                )
        )
    }

    fun remove(server: CollectServer) {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapCompletable { dataSource: DataSource ->
                    dataSource.removeCollectServer(
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

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}
