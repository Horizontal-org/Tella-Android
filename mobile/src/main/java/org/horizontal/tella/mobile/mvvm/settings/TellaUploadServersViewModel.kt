package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import org.horizontal.tella.mobile.mvvm.base.BaseSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class TellaUploadServersViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource
) : BaseSettingsViewModel() {

    private val _listTellaReportServers = SingleLiveEvent<List<TellaReportServer>>()
    val listTellaReportServers: LiveData<List<TellaReportServer>> = _listTellaReportServers

    private val _createdServer = SingleLiveEvent<TellaReportServer>()
    val createdServer: LiveData<TellaReportServer> = _createdServer

    private val _updatedServer = SingleLiveEvent<TellaReportServer>()
    val updatedServer: LiveData<TellaReportServer> = _updatedServer

    private val _removedServer = SingleLiveEvent<TellaReportServer>()
    val removedServer: LiveData<TellaReportServer> = _removedServer


    fun getTellaUploadServers() {
        disposables.add(
            keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle(Function<DataSource, SingleSource<List<TellaReportServer>>> { it.listTellaUploadServers() })
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { servers -> _listTellaReportServers.postValue(servers) },
                    { throwable ->
                        handleError(throwable)
                    }
                )
        )
    }

    fun create(server: TellaReportServer) {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle {
                    it.createTellaUploadServer(
                        server
                    )
                }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { createdServer -> _createdServer.postValue(createdServer) },
                    { throwable ->
                        handleError(throwable, R.string.settings_docu_toast_fail_create_server)
                    }
                )
        )
    }

    fun update(server: TellaReportServer) {
        disposables.add(
            keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle(Function<DataSource, SingleSource<TellaReportServer>> {
                    it.updateTellaUploadServer(
                        server
                    )
                })
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

    fun remove(server: TellaReportServer) {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapCompletable { it.removeTellaServerAndResources(server.id) }
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
        super.onCleared()
        disposables.dispose()
    }

}