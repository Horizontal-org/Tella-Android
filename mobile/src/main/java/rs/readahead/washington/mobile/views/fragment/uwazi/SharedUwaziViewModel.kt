package rs.readahead.washington.mobile.views.fragment.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.UwaziDataSource
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.EntityInstanceBundle
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.ViewEntityInstanceItem
import rs.readahead.washington.mobile.views.fragment.uwazi.mappers.toViewEntityInstanceItem
import rs.readahead.washington.mobile.views.fragment.uwazi.mappers.toViewEntityTemplateItem

class SharedUwaziViewModel : ViewModel() {

    var error = MutableLiveData<Throwable>()
    private val _templates = MutableLiveData<List<Any>>()
    val templates: LiveData<List<Any>> get() = _templates
    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    private var _showSheetMore = SingleLiveEvent<CollectTemplate>()
    val showSheetMore: LiveData<CollectTemplate> get() = _showSheetMore
    private var _openEntity = SingleLiveEvent<CollectTemplate>()
    val openEntity: LiveData<CollectTemplate> get() = _openEntity
    private var _openEntityInstance = SingleLiveEvent<UwaziEntityInstance>()
    val openEntityInstance: LiveData<UwaziEntityInstance> get() = _openEntityInstance
    private var _showInstanceSheetMore = SingleLiveEvent<UwaziEntityInstance>()
    val showInstanceSheetMore: LiveData<UwaziEntityInstance> get() = _showInstanceSheetMore
    private val _draftInstances = MutableLiveData<List<ViewEntityInstanceItem>>()
    val draftInstances: LiveData<List<ViewEntityInstanceItem>> get() = _draftInstances
    private val _submittedInstances = MutableLiveData<List<ViewEntityInstanceItem>>()
    val submittedInstances: LiveData<List<ViewEntityInstanceItem>> get() = _submittedInstances
    private val _outboxInstances = MutableLiveData<List<ViewEntityInstanceItem>>()
    val outboxInstances: LiveData<List<ViewEntityInstanceItem>> get() = _outboxInstances
    private var _instanceDeleteD = SingleLiveEvent<Boolean>()
    val instanceDeleteD: LiveData<Boolean> get() = _instanceDeleteD
    var onInstanceSuccess = SingleLiveEvent<UwaziEntityInstance>()
    var onGetInstanceError = SingleLiveEvent<Throwable>()

    init {
        listTemplates()
    }

    fun listTemplates() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listBlankTemplates().toObservable()
            }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                { templates: List<CollectTemplate> ->
                    val resultList = mutableListOf<Any>()
                    resultList.add(0,"These are Uwazi templates you can fill out.")
                    templates.map {
                        resultList.add(it.toViewEntityTemplateItem(onMoreClicked = {
                            onMoreClicked(
                                it
                            )
                        }, onFavoriteClicked = { toggleFavorite(it) },
                           onOpenEntityClicked = {openEntity(it)}
                            ))
                    }
                    _templates.postValue(resultList)
                }
            ) { throwable: Throwable? ->
                error.postValue(
                    throwable
                )
            }
        )
    }

    fun listDrafts() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listDraftInstances().toObservable()
            }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                { drafts: List<UwaziEntityInstance> ->
                    val resultList = mutableListOf<ViewEntityInstanceItem>()
                    drafts.map {
                        resultList.add(it.toViewEntityInstanceItem (onMoreClicked = { onInstanceMoreClicked(it) },
                        onOpenClicked = {openEntityInstance(it)}
                            ))
                    }
                    _draftInstances.postValue(resultList)
                }
            ) { throwable: Throwable? ->
                error.postValue(
                    throwable
                )
            }
        )
    }

    fun listSubmitted() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listSubmittedInstances().toObservable()
            }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                { drafts: List<UwaziEntityInstance> ->
                    val resultList = mutableListOf<ViewEntityInstanceItem>()
                    drafts.map {
                        resultList.add(it.toViewEntityInstanceItem(
                            onMoreClicked = { onInstanceMoreClicked(it) },
                            onOpenClicked = { getInstanceUwaziEntity(it.id) }
                            )
                        )
                    }
                    _submittedInstances.postValue(resultList)
                }
            ) { throwable: Throwable? ->
                error.postValue(
                    throwable
                )
            }
        )
    }

    fun listOutBox() {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listOutboxInstances().toObservable()
            }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                { outboxes: List<UwaziEntityInstance> ->
                    val resultList = mutableListOf<ViewEntityInstanceItem>()
                    outboxes.map {
                        resultList.add(it.toViewEntityInstanceItem (onMoreClicked = {
                            onInstanceMoreClicked(
                                it
                            )
                        },
                            onOpenClicked = {openEntityInstance(it)}
                            ))
                    }
                    _outboxInstances.postValue(resultList)
                }
            ) { throwable: Throwable? ->
                error.postValue(
                    throwable
                )
            }
        )
    }

    private fun onMoreClicked(template: CollectTemplate) {
        _showSheetMore.postValue(template)
    }

    private fun onInstanceMoreClicked(instance: UwaziEntityInstance) {
        _showInstanceSheetMore.postValue(instance)
    }

    private fun toggleFavorite(template: CollectTemplate) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle { dataSource: UwaziDataSource -> dataSource.toggleFavorite(template) }
            .subscribe({ listTemplates() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                error.postValue(throwable)
            }
        )
    }

    fun confirmDelete(template: CollectTemplate) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMapCompletable { dataSource: UwaziDataSource ->
                dataSource.deleteTemplate(
                    template.id
                )
            }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                { listTemplates() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                error.postValue(throwable)
            }
        )
    }

    fun confirmDelete(instance: UwaziEntityInstance) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMapCompletable { dataSource: UwaziDataSource ->
                dataSource.deleteEntityInstance(
                    instance.id
                )
            }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                {
                    _instanceDeleteD.postValue(true)
                }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                error.postValue(throwable)
            }
        )
    }

    private fun openEntity(template: CollectTemplate){
        _openEntity.postValue(template)
    }

    private fun openEntityInstance(entity: UwaziEntityInstance){
        _openEntityInstance.postValue(entity)
    }

    fun getInstanceUwaziEntity(instanceId: Long) {
        var uwaziEntityInstance: UwaziEntityInstance? = null
        disposables.add(keyDataSource.uwaziDataSource
            .flatMapSingle { dataSource: UwaziDataSource ->
                dataSource.getBundle(
                    instanceId
                )
            }
            .flatMapSingle { bundle: EntityInstanceBundle ->
                uwaziEntityInstance = bundle.instance
                MyApplication.rxVault.get(bundle.fileIds)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ vaultFiles: List<VaultFile> ->
                val widgetMediaFiles = mutableListOf<FormMediaFile>()
                for (file in vaultFiles) {
                    widgetMediaFiles.add(FormMediaFile.fromMediaFile(file))
                }
                uwaziEntityInstance?.widgetMediaFiles = widgetMediaFiles
                onInstanceSuccess.postValue(
                    uwaziEntityInstance?.let { maybeCloneInstance(it) }
                )
            }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                onGetInstanceError.postValue(throwable)
            }
        )
    }

    private fun maybeCloneInstance(instance: UwaziEntityInstance): UwaziEntityInstance {
        /*if (instance.status == UwaziEntityStatus.SUBMITTED) {
            instance.clonedId = instance.id // we are clone of submitted form
            instance.id = 0
            instance.status = UwaziEntityStatus.UNKNOWN
            instance.updated = 0
            //instance.title = instance.title
            for (mediaFile in instance.widgetMediaFiles) {
                mediaFile.status = FormMediaFileStatus.UNKNOWN
            }
        }*/
        return instance
    }

}