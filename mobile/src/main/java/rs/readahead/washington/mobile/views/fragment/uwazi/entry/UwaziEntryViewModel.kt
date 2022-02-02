package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.UwaziDataSource
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance


class UwaziEntryViewModel : ViewModel(){

    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()
    var error = MutableLiveData<Throwable>()
    private val _progress = MutableLiveData<Boolean>()
    val progress : LiveData<Boolean> get() = _progress
    private val _instance = MutableLiveData<UwaziEntityInstance>()
    val instance: LiveData<UwaziEntityInstance> get() = _instance
    private val _template = MutableLiveData<CollectTemplate>()
    val template: LiveData<CollectTemplate> get() = _template

    fun getBlankTemplate(templateID : String) {
        keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { dataSource: UwaziDataSource ->
                dataSource.getBlankCollectTemplateById(templateID).toObservable()
            }
            ?.subscribe(
                { template: CollectTemplate? ->
                    if (template != null) {
                        _template.postValue(template)
                    }
                },
                { throwable: Throwable? ->
                    FirebaseCrashlytics.getInstance().recordException(throwable!!)
                    error.postValue(throwable)
                }
            )?.let {
                disposables.add(
                    it
                )
            }
    }

    fun saveEntityInstance(instance : UwaziEntityInstance) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMap { dataSource: UwaziDataSource -> dataSource.saveEntityInstance(instance).toObservable() }
            .doFinally { _progress.postValue(false)  }
            .subscribe ({ savedInstance ->
                _instance.postValue(savedInstance)
            }

            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
                error.postValue(throwable)
            })
    }

    fun prepareUwaziEntityRequest(collectTemplate: CollectTemplate){

    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}