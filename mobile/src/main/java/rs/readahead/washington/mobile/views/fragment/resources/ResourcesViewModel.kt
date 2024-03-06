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
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.ResourceDataSource
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.entity.resources.ListResourceResult
import rs.readahead.washington.mobile.domain.entity.resources.Resource
import rs.readahead.washington.mobile.domain.exception.NotFountException
import rs.readahead.washington.mobile.domain.repository.resources.ResourcesRepository
import rs.readahead.washington.mobile.media.MediaFileHandler
import javax.inject.Inject

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val resourcesRepository: ResourcesRepository
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress

    private val _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int> get() = _downloadProgress

    private val _resources = MutableLiveData<List<Resource>>()
    val resources: LiveData<List<Resource>> get() = _resources

    private val _downloadedResource = MutableLiveData<Resource>()
    val downloadedResource: LiveData<Resource> = _downloadedResource

    private val _savedResources = MutableLiveData<List<Resource>>()
    val savedResources: LiveData<List<Resource>> = _savedResources

    private val _pdfFile = MutableLiveData<VaultFile>()
    val pdfFile: LiveData<VaultFile> = _pdfFile

    private var _error = MutableLiveData<Throwable?>()
    val error: MutableLiveData<Throwable?> get() = _error

    private val _deletedResource = MutableLiveData<String>()
    val deletedResource: LiveData<String> get() = _deletedResource

    fun getResources() {
        val projectMap = HashMap<String, Long>()
        val urlProjects = HashMap<String, ArrayList<TellaReportServer>>()
        disposables.add(keyDataSource.resourceDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: ResourceDataSource ->
                dataSource.listTellaUploadServers().toObservable()
            }
            .flatMap { projects: List<TellaReportServer> ->
                // Save server id to download resource of the Project
                val urls = ArrayList<String>()
                projects.forEach { project ->
                    if (!urls.contains(project.url)) {
                        urls.add(project.url)
                    }
                    projectMap[project.projectId] = project.id
                }

                urls.forEach { url ->
                    val projectList = ArrayList<TellaReportServer>()
                    projects.forEach { project ->
                        if (project.url.equals(url)) {
                            projectList.add(project)
                        }
                    }
                    urlProjects[url] = projectList
                }

                val singles: MutableList<Single<ListResourceResult>> =
                    ArrayList()

                urlProjects.forEach {
                    // We are making a call for each distinct url and it's projects
                    singles.add(resourcesRepository.getAllResourcesResult(it.key, it.value))
                }

                Single.zip(
                    singles
                ) { lists: Array<Any?> ->
                    val allResults = ListResourceResult()
                    for (list in lists) {
                        if (list is ListResourceResult) {
                            val slugs =
                                list.slugs
                            val errors =
                                list.errors
                            allResults.slugs += slugs
                            allResults.errors += errors
                        }
                    }
                    allResults
                }.toObservable()
            }
            .doFinally { _progress.postValue(false) }
            .subscribe({
                val resourcesList = mutableListOf<Resource>()
                it.slugs.map { instance ->
                    instance.resources.forEach { resource: Resource ->
                        resource.project = instance.name
                        if (projectMap[instance.id] != null) {
                            resource.serverId = projectMap[instance.id]!!
                        }
                    }
                    resourcesList.addAll(instance.resources)
                }
                _resources.postValue(resourcesList)
            })
            { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
                _error.postValue(throwable)
            })
    }

    fun downloadResource(resource: Resource) {
        lateinit var resourceDataSource : ResourceDataSource
        disposables.add(keyDataSource.resourceDataSource
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { _downloadProgress.postValue(1) }
            .flatMap { dataSource: ResourceDataSource ->
                resourceDataSource = dataSource
                dataSource.getTellaUploadServer(resource.serverId).toObservable()
            }
            .flatMap {
                resourcesRepository.downloadResourceByFileName(it, resource.fileName).toObservable()
            }
            .flatMap {
                MediaFileHandler.downloadResourcePdfInputstream(
                    it.byteStream(),
                    resource.fileName,
                    null
                ).toObservable()
            }
            .flatMap {
                resource.fileId = it.id
                resourceDataSource.saveResource(resource).toObservable()
            }
            .doFinally {
                _downloadProgress.postValue(-1)
            }
            .subscribe({
                _downloadedResource.postValue(resource)
            })
            { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
                _error.postValue(throwable)
            })
    }

    fun getPdfFile(id: String?) {
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
        disposables.add(keyDataSource.resourceDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            //.doOnSubscribe { _progress.postValue(true) }
            .flatMapSingle<List<Resource>> { obj: ResourceDataSource -> obj.listResources() }
            // .doFinally { _progress.postValue(false) }
            .subscribe(
                { list: List<Resource> ->
                    _savedResources.postValue(list)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            }
        )
    }

    fun removeResource(resource: Resource) {
        disposables.add(keyDataSource.resourceDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMapSingle { dataSource: ResourceDataSource ->
                dataSource.deleteResource(
                    resource
                )
            }
            .flatMapSingle { MyApplication.rxVault[it] }
            .flatMapSingle { MyApplication.rxVault.delete(it) }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                {
                    if (it) _deletedResource.postValue(resource.fileName)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                _error.postValue(throwable)
            }
        )
    }

    fun dispose() {
        disposables.dispose()
    }

    override fun onCleared() {
        super.onCleared()
        dispose()
    }
}

