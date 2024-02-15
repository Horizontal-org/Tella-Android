package rs.readahead.washington.mobile.views.fragment.resources

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.entity.resources.Resource
import rs.readahead.washington.mobile.domain.exception.NotFountException
import rs.readahead.washington.mobile.domain.repository.resources.ResourcesRepository
import rs.readahead.washington.mobile.domain.usecases.reports.GetReportsServersUseCase
import rs.readahead.washington.mobile.media.MediaFileHandler
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

    private val _resources = MutableLiveData<List<Resource>>()
    val resources: LiveData<List<Resource>> get() = _resources
    val progress: LiveData<Boolean> get() = _progress
    private val _serversList = MutableLiveData<List<TellaReportServer>>()
    val serversList: LiveData<List<TellaReportServer>> get() = _serversList

    private val _downloadedResource = MutableLiveData<Resource>()
    val downloadedResource: LiveData<Resource> = _downloadedResource

    private val _savedResources = MutableLiveData<List<Resource>>()
    val savedResources: LiveData<List<Resource>> = _savedResources

    private val _pdfFile = MutableLiveData<VaultFile>()
    val pdfFile: LiveData<VaultFile> = _pdfFile

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
            .doFinally { _progress.postValue(false) }
            .subscribe({
                val resourcesList = mutableListOf<Resource>()
                it.map { instance ->
                    resourcesList.addAll(instance.resources)
                }
                resourcesList.forEach {
                    Timber.d(
                        "++++ resource %s, %s, %s",
                        it.fileName,
                        it.createdAt,
                        it.size
                    )
                }
                _resources.postValue(resourcesList)
            })
            { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
            })
    }

    fun downloadResource(resource: Resource) {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { _progress.postValue(true) }
            .doFinally {
                _progress.postValue(false)
            }
            .flatMap { dataSource: DataSource ->
                dataSource.listTellaUploadServers().toObservable()
            }
            .flatMap { servers: List<TellaReportServer> ->
                resourcesRepository.downloadResource(servers[0], resource.fileName).toObservable()
            }
            .flatMap {
                MediaFileHandler.downloadPdfInputstream(
                    it.byteStream(),
                    resource.fileName,
                    null
                ).toObservable()
            }
            .flatMap {
                resource.fileId = it.id
                dataSource.saveResource(resource).toObservable()
            }
            .subscribe({
                _downloadedResource.postValue(resource)
            })
            { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
            })
    }

    fun getMediaFile(id: String?) {
        disposables.add(Single
            .fromCallable<Single<VaultFile>> {
                MyApplication.rxVault[id]
            }
            .doOnSubscribe { _progress.postValue(true) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                _progress.postValue(false)
            }
            .subscribe(
                { vaultFile: Single<VaultFile>? ->
                    if (vaultFile == null) {
                        _error.postValue(NotFountException())
                    } else {
                        _pdfFile.postValue(vaultFile.blockingGet())
                    }
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
            })
    }

    fun listResources() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMapSingle<List<Resource>> { obj: DataSource -> obj.listResources() }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                { list: List<Resource> ->
                    _savedResources.postValue(list)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable!!)
            }
        )
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

