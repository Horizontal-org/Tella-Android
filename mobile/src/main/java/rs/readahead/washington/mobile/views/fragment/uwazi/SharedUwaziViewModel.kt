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
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.ViewEntityTemplateItem
import rs.readahead.washington.mobile.views.fragment.uwazi.mappers.toViewEntityTemplateItem

class SharedUwaziViewModel : ViewModel() {

    private val uwaziRepository by lazy { UwaziRepository() }
    var error = MutableLiveData<Throwable>()
    private val _templates = MutableLiveData<List<ViewEntityTemplateItem>>()
    val templates: LiveData<List<ViewEntityTemplateItem>> get() = _templates
    private val _progress = MutableLiveData<Boolean>()
    val progress : LiveData<Boolean> get() = _progress
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    private var  _showSheetMore = SingleLiveEvent<CollectTemplate>()
    val showSheetMore : LiveData<CollectTemplate> get() = _showSheetMore

    init {
        listTemplates()
    }

     fun listTemplates(){
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listBlankTemplates().toObservable()
            }
            .doFinally { _progress.postValue(false)  }
            .subscribe(
                { templates : List<CollectTemplate> ->
                    val resultList = mutableListOf<ViewEntityTemplateItem>()
                    templates.map {
                        resultList.add(it.toViewEntityTemplateItem(onMoreClicked = {onMoreClicked(it)}, onFavoriteClicked = {onFavoriteClicked()}))
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

   private fun onMoreClicked(template: CollectTemplate){
        _showSheetMore.postValue(template)
    }


    fun onFavoriteClicked(){

    }

    fun confirmDelete(template : CollectTemplate){
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMapCompletable { dataSource: UwaziDataSource -> dataSource.deleteTemplate(template.id) }
            .doFinally { _progress.postValue(false)  }
            .subscribe(
                {listTemplates()}
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
                error.postValue(throwable)
            }
        )
    }

}