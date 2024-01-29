package rs.readahead.washington.mobile.views.fragment.resources

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.domain.entity.reports.ResourceTemplate
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.resources.ResourcesRepository
import rs.readahead.washington.mobile.domain.usecases.reports.GetReportsServersUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val getReportsServersUseCase: GetReportsServersUseCase,
    private val resourcesRepository: ResourcesRepository,
    private val dataSource: DataSource
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val _progress = MutableLiveData<Boolean>()

    private val _resources = MutableLiveData<List<ResourceTemplate>>()
    val resources: LiveData<List<ResourceTemplate>> get() = _resources
    val progress: LiveData<Boolean> get() = _progress
    private val _serversList = MutableLiveData<List<TellaReportServer>>()
    val serversList: LiveData<List<TellaReportServer>> get() = _serversList
    private var _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> get() = _error

    fun listServers() {
        _progress.postValue(true)
        getReportsServersUseCase.execute(onSuccess = { result ->
            _serversList.postValue(result)
        }, onError = {
            _error.postValue(it)
        }, onFinished = {
            _progress.postValue(false)
        })
    }

    fun getResources() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: DataSource ->
                dataSource.listTellaUploadServers().toObservable()
            }
            .flatMap { servers: List<TellaReportServer> ->
                resourcesRepository.getResourcesResult(servers).toObservable()
            }
            .subscribe({
                val resourcesList = mutableListOf<ResourceTemplate>()
                it.map { instance ->
                    resourcesList.addAll(instance.resources)
                }
                resourcesList.forEach {
                    Timber.d("++++ id %s", it.id)
                    Timber.d("++++ name %s", it.fileName)
                    Timber.d("++++ size %s", it.size)
                    Timber.d("++++ created %s", it.createdAt)
                }
                _resources.postValue(resourcesList)
            })
            { throwable: Throwable? ->
                Timber.d("+++ %s", throwable.toString())
                /*FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )*/
            })
    }

    fun dispose() {
        disposables.dispose()
    }

    fun clearDisposable() {
        resourcesRepository.getDisposable().clear()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
        resourcesRepository.cleanup()
    }
}

