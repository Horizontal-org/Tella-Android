package rs.readahead.washington.mobile.views.fragment.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.UwaziDataSource
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.ViewEntityInstanceItem
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.uwazi.mappers.toViewEntityInstanceItem
import rs.readahead.washington.mobile.views.fragment.uwazi.mappers.toViewEntityTemplateItem

class SharedUwaziViewModel : ViewModel() {

    var error = MutableLiveData<Throwable>()
    private val _templates = MutableLiveData<List<ViewEntityTemplateItem>>()
    val templates: LiveData<List<ViewEntityTemplateItem>> get() = _templates
    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    private var _showSheetMore = SingleLiveEvent<CollectTemplate>()
    val showSheetMore: LiveData<CollectTemplate> get() = _showSheetMore
    private var _showInstanceSheetMore = SingleLiveEvent<UwaziEntityInstance>()
    val showInstanceSheetMore: LiveData<UwaziEntityInstance> get() = _showInstanceSheetMore
    private val _draftInstances = MutableLiveData<List<ViewEntityInstanceItem>>()
    val draftInstances: LiveData<List<ViewEntityInstanceItem>> get() = _draftInstances
    private val _submittedInstances = MutableLiveData<List<ViewEntityInstanceItem>>()
    val submittedInstances: LiveData<List<ViewEntityInstanceItem>> get() = _submittedInstances

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
                    val resultList = mutableListOf<ViewEntityTemplateItem>()
                    templates.map {
                        resultList.add(it.toViewEntityTemplateItem(onMoreClicked = {
                            onMoreClicked(
                                it
                            )
                        }, onFavoriteClicked = { toggleFavorite(it) }))
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
                        resultList.add(it.toViewEntityInstanceItem (onMoreClicked = {
                            onInstanceMoreClicked(
                                it
                            )
                        }))
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
                        resultList.add(it.toViewEntityInstanceItem (onMoreClicked = {
                            onInstanceMoreClicked(
                                it
                            )
                        }))
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
                { listDrafts() }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                error.postValue(throwable)
            }
        )
    }

}