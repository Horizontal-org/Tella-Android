package rs.readahead.washington.mobile.views.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
//import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.UwaziDataSource
import rs.readahead.washington.mobile.data.entity.uwazi.LanguageEntity
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.views.adapters.uwazi.ViewLanguageItem
import rs.readahead.washington.mobile.views.fragment.uwazi.mappers.toViewLanguageItem
import timber.log.Timber

class UwaziServerLanguageViewModel : ViewModel() {

    private val repository = UwaziRepository()
    private val disposables = CompositeDisposable()
    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val _listLanguage = MutableLiveData<List<ViewLanguageItem>>()
    val listLanguage: LiveData<List<ViewLanguageItem>> get() = _listLanguage
    var error = MutableLiveData<Throwable>()
    private val _languageClicked = MutableLiveData<LanguageEntity>()
    val languageClicked: LiveData<LanguageEntity> get() = _languageClicked
    private val _languageUpdated = MutableLiveData<Boolean>()
    val languageUpdated: LiveData<Boolean> get() = _languageUpdated
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()


    fun getServerLanguage(server: UWaziUploadServer) {
        disposables.add(repository.getSettings(server)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .doFinally { _progress.postValue(false) }
            .subscribe({
                val list = mutableListOf<ViewLanguageItem>()
                it.map { language ->
                  //  list.add(language.toViewLanguageItem { onLanguageClicked(language) })
                }
                _listLanguage.postValue(list)
            }
            ) { throwable: Throwable? ->
                Timber.d(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
                error.postValue(throwable)
            })
    }

    private fun onLanguageClicked(language: LanguageEntity){
        _languageClicked.postValue(language)
    }

    fun updateLanguageSettings(server: UWaziUploadServer,language: LanguageEntity) {
        disposables.add(keyDataSource.uwaziDataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .flatMapSingle { dataSource: UwaziDataSource ->
                server.localeCookie = language.key
                dataSource.updateUwaziServer(server)
            }
            .doFinally { _progress.postValue(false) }
            .subscribe(
                { _ -> _languageUpdated.postValue(true) }
            ) { throwable: Throwable? ->
                Timber.d(throwable!!)
                error.postValue(throwable)
            }
        )
    }

}