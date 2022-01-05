package rs.readahead.washington.mobile.views.fragment.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import timber.log.Timber

class SharedUwaziViewModel : ViewModel() {
    private val uwaziRepository by lazy { UwaziRepository() }
    var error = MutableLiveData<String>()
    private val _templates = MutableLiveData<HashMap<UWaziUploadServer,List<UwaziEntityRow>>>()
    val templates: LiveData<HashMap<UWaziUploadServer,List<UwaziEntityRow>>> get() = _templates
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()

    private fun getTemplates(servers: List<UWaziUploadServer>) {
        val list = HashMap<UWaziUploadServer,List<UwaziEntityRow>>()
        viewModelScope.launch {
            if (!servers.isNullOrEmpty()) {
                servers.asFlow().map { server ->
                    uwaziRepository.getTemplates(server)
                        .catch {
                            Timber.d(it)
                        }
                        .collect {
                            Timber.d(it.toString())
                            list[server] = it.rows
                        }
                }.catch { e -> error.postValue(e.toString())  }
                    .collect {
                        _templates.postValue(list)
                    }

            }
        }

    }

    fun getServers() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { }
            .flatMapSingle { obj: DataSource -> obj.listUwaziServers() }
            .subscribe(
                { list: List<UWaziUploadServer> -> getTemplates(list) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
            }
        )
    }
}