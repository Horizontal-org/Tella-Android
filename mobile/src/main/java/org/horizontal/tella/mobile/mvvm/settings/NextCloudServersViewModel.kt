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
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.database.NextCloudDataSource
import org.horizontal.tella.mobile.data.openrosa.OpenRosaService
import org.horizontal.tella.mobile.domain.entity.nextcloud.NextCloudServer
import javax.inject.Inject

@HiltViewModel
class NextCloudServersViewModel @Inject constructor(private val keyDataSource: KeyDataSource) :
    ViewModel() {

    private val disposables = CompositeDisposable()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _listNextCloudServers = MutableLiveData<List<NextCloudServer>>()
    val listNextCloudServers: LiveData<List<NextCloudServer>> get() = _listNextCloudServers

    private val _serverCreated = MutableLiveData<NextCloudServer>()
    val serverCreated: LiveData<NextCloudServer> get() = _serverCreated

    private val _serverRemoved = MutableLiveData<NextCloudServer>()
    val serverRemoved: LiveData<NextCloudServer> get() = _serverRemoved

    private val _error = MutableLiveData<Int>()
    val error: LiveData<Int> get() = _error

    fun getNextCloudServers() {
        _loading.value = true
        disposables.add(
            keyDataSource.nextCloudDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle { dataSource: NextCloudDataSource -> dataSource.listNextCloudServers() }
                .doFinally { _loading.postValue(false) }
                .subscribe(
                    { servers -> _listNextCloudServers.postValue(servers) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                     //   _error.postValue(throwable)
                    }
                )
        )
    }

    fun create(server: NextCloudServer) {
        _loading.value = true
        disposables.add(
            keyDataSource.nextCloudDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle { dataSource: NextCloudDataSource -> dataSource.saveNextCloudServer(server) }
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

    fun remove(server: NextCloudServer) {
        _loading.value = true
        disposables.add(
            keyDataSource.nextCloudDataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable { dataSource: NextCloudDataSource -> dataSource.removeNextCloudServer(server.id) }
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