package rs.readahead.washington.mobile.views.fragment.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
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
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()

    init {
        listTemplates()
    }

     fun listTemplates(){
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.listBlankTemplates().toObservable()
            }
            .subscribe(
                { templates : List<CollectTemplate> ->
                    val resultList = mutableListOf<ViewEntityTemplateItem>()
                    templates.map {
                        resultList.add(it.toViewEntityTemplateItem(onDownloadClicked = {onDownloadClicked()}, onFavoriteClicked = {onFavoriteClicked()}))
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

    fun onDownloadClicked(){

    }


    fun onFavoriteClicked(){

    }

}