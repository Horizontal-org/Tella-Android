package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer
import javax.inject.Inject

class TellaUploadServersViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _listTellaReportServers = MutableLiveData<List<TellaReportServer>>()
    val listTellaReportServers: LiveData<List<TellaReportServer>> = _listTellaReportServers

    private val _createdServer = MutableLiveData<TellaReportServer>()
    val createdServer: LiveData<TellaReportServer> = _createdServer

    private val _updatedServer = MutableLiveData<TellaReportServer>()
    val updatedServer: LiveData<TellaReportServer> = _updatedServer

    private val _removedServer = MutableLiveData<TellaReportServer>()
    val removedServer: LiveData<TellaReportServer> = _removedServer

    private val _error = MutableLiveData<Int>()
    val error: LiveData<Int> = _error

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
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                       // _error.postValue(throwable)
                    }
                )
        )
    }

    fun create(server: TellaReportServer) {
        disposables.add(
            keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { _loading.postValue(true) }
                .flatMapSingle(Function<DataSource, SingleSource<TellaReportServer>> {
                    it.createTellaUploadServer(
                        server
                    )
                })
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { createdServer -> _createdServer.postValue(createdServer) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.settings_docu_toast_fail_create_server)
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
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.settings_docu_toast_fail_update_server)
                    }
                )
        )
    }

    fun remove(server: TellaReportServer) {
        disposables.add(
            keyDataSource.getDataSource()
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
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.settings_docu_toast_fail_delete_server)
                    }
                )
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }

}