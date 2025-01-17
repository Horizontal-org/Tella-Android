package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.database.DropBoxDataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer
import javax.inject.Inject

@HiltViewModel
class DropBoxServersViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource,
) : ViewModel() {
    private val disposables = CompositeDisposable()

    // LiveData to observe changes
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _listDropBoxServers = MutableLiveData<List<DropBoxServer>>()
    val listDropBoxServers: LiveData<List<DropBoxServer>> get() = _listDropBoxServers

    private val _error = MutableLiveData<Int>()
    val error: LiveData<Int> get() = _error

    private val _serverCreated = MutableLiveData<DropBoxServer>()
    val serverCreated: LiveData<DropBoxServer> get() = _serverCreated

    private val _serverRemoved = MutableLiveData<DropBoxServer>()
    val serverRemoved: LiveData<DropBoxServer> get() = _serverRemoved

    fun getDropBoxServers() {
        _loading.value = true
        disposables.add(
            keyDataSource.dropBoxDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle { dataSource: DropBoxDataSource -> dataSource.listDropBoxServers() }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { servers -> _listDropBoxServers.postValue(servers) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                     //   _error.postValue(throwable)
                    }
                )
        )
    }

    fun create(server: DropBoxServer) {
        _loading.value = true
        disposables.add(
            keyDataSource.dropBoxDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle { dataSource: DropBoxDataSource ->
                    dataSource.saveDropBoxServer(
                        server
                    )
                }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { createdServer -> _serverCreated.postValue(createdServer) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _error.postValue(R.string.settings_docu_toast_fail_create_server)
                    }
                )
        )
    }

    fun remove(server: DropBoxServer) {
        _loading.value = true
        disposables.add(
            keyDataSource.dropBoxDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable { dataSource: DropBoxDataSource ->
                    dataSource.removeDropBoxServer(
                        server.id
                    )
                }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    {
                        OpenRosaService.clearCache()
                        _serverRemoved.postValue(server)
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