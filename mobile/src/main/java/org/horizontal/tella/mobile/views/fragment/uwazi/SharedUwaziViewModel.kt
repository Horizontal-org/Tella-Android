package org.horizontal.tella.mobile.views.fragment.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.database.UwaziDataSource
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.domain.entity.uwazi.EntityInstanceBundle
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance
import org.horizontal.tella.mobile.views.fragment.uwazi.adapters.ViewEntityInstanceItem
import org.horizontal.tella.mobile.views.fragment.uwazi.mappers.toViewEntityInstanceItem
import org.horizontal.tella.mobile.views.fragment.uwazi.mappers.toViewEntityTemplateItem

class SharedUwaziViewModel : ViewModel() {

    var error = MutableLiveData<Throwable?>()
    private val _templates = MutableLiveData<List<Any>>()
    val templates: LiveData<List<Any>> get() = _templates
    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    private var _showSheetMore = SingleLiveEvent<UwaziTemplate>()
    val showSheetMore: LiveData<UwaziTemplate> get() = _showSheetMore
    private var _openEntity = SingleLiveEvent<UwaziTemplate>()
    val openEntity: LiveData<UwaziTemplate> get() = _openEntity
    private var _openEntityInstance = SingleLiveEvent<UwaziEntityInstance>()
    val openEntityInstance: LiveData<UwaziEntityInstance> get() = _openEntityInstance
    private var _showInstanceSheetMore = SingleLiveEvent<UwaziEntityInstance>()
    val showInstanceSheetMore: LiveData<UwaziEntityInstance> get() = _showInstanceSheetMore
    private val _draftInstances = SingleLiveEvent<List<Any>>()
    val draftInstances: LiveData<List<Any>> get() = _draftInstances
    private val _submittedInstances = SingleLiveEvent<List<Any>>()
    val submittedInstances: LiveData<List<Any>> get() = _submittedInstances
    private val _outboxInstances = SingleLiveEvent<List<Any>>()
    val outboxInstances: LiveData<List<Any>> get() = _outboxInstances
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
                { templates: List<UwaziTemplate> ->
                    val resultList = mutableListOf<Any>()
                    resultList.add(0, R.string.Uwazi_Templates_HeaderMessage)
                    templates.map {
                        resultList.add(it.toViewEntityTemplateItem(onMoreClicked = {
                            onMoreClicked(
                                it
                            )
                        }, onFavoriteClicked = { toggleFavorite(it) },
                            onOpenEntityClicked = { openEntity(it) }
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
                        resultList.add(it.toViewEntityInstanceItem(onMoreClicked = {
                            onInstanceMoreClicked(
                                it
                            )
                        },
                            onOpenClicked = { openEntityInstance(it) }
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
                        resultList.add(it.toViewEntityInstanceItem(onMoreClicked = {
                            onInstanceMoreClicked(
                                it
                            )
                        },
                            onOpenClicked = { openEntityInstance(it) }
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

    private fun onMoreClicked(template: UwaziTemplate) {
        _showSheetMore.postValue(template)
    }

    private fun onInstanceMoreClicked(instance: UwaziEntityInstance) {
        _showInstanceSheetMore.postValue(instance)
    }

    private fun toggleFavorite(template: UwaziTemplate) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle { dataSource: UwaziDataSource -> dataSource.toggleFavorite(template) }
            .subscribe({ listTemplates() }
            ) { throwable: Throwable ->
                FirebaseCrashlytics.getInstance().recordException(throwable)
                error.postValue(throwable)
            }
        )
    }

    fun confirmDelete(template: UwaziTemplate) {
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

    private fun openEntity(template: UwaziTemplate) {
        _openEntity.postValue(template)
    }

    private fun openEntityInstance(entity: UwaziEntityInstance) {
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
            }) { throwable: Throwable ->
                FirebaseCrashlytics.getInstance().recordException(throwable)
                onGetInstanceError.postValue(throwable)
            }
        )
    }

    private fun maybeCloneInstance(instance: UwaziEntityInstance): UwaziEntityInstance {
        /*if (instance.status == EntityStatus.SUBMITTED) {
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