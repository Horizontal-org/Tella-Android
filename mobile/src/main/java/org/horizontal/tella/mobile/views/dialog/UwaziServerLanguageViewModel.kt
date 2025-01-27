package org.horizontal.tella.mobile.views.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.entity.uwazi.LanguageEntity
import org.horizontal.tella.mobile.data.repository.UwaziRepository
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.views.adapters.uwazi.ViewLanguageItem

internal const val TITLE_KEY = "tk"
internal const val ID_KEY = "ik"
internal const val OBJECT_KEY = "ok"

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
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
                error.postValue(throwable)
            })
    }
}