package org.horizontal.tella.mobile.views.dialog.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.repository.UwaziRepository
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.uwazi.Language
import org.horizontal.tella.mobile.views.adapters.uwazi.ViewLanguageItem
import org.horizontal.tella.mobile.views.fragment.uwazi.mappers.toViewLanguageItem

private const val TWO_FACTOR_AUTHENTICATION_CODE = 409
private const val AUTHENTICATION_FAILED = 401

class UwaziConnectFlowViewModel : ViewModel() {
    private val repository by lazy { UwaziRepository() }
    private val disposables = CompositeDisposable()
    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> get() = _progress
    private val _isPublic = SingleLiveEvent<Boolean>()
    var error = MutableLiveData<Throwable>()
    val isPublic: LiveData<Boolean> get() = _isPublic
    private val _twoFactorAuthentication = SingleLiveEvent<Boolean>()
    val twoFactorAuthentication: LiveData<Boolean> get() = _twoFactorAuthentication
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val _languageUpdated = MutableLiveData<Boolean>()
    val languageUpdated: LiveData<Boolean> get() = _languageUpdated
    private val _settings = SingleLiveEvent<Pair<String, List<ViewLanguageItem>>>()
    val settings: LiveData<Pair<String, List<ViewLanguageItem>>> get() = _settings
    private val _languageClicked = MutableLiveData<Language>()
    val languageClicked: LiveData<Language> get() = _languageClicked
    private val _authenticationError = SingleLiveEvent<Boolean>()
    val authenticationError: LiveData<Boolean> get() = _authenticationError
    private val _authenticationSuccess = SingleLiveEvent<Boolean>()
    val authenticationSuccess: LiveData<Boolean> get() = _authenticationSuccess

    fun getServerLanguage(url: String) {
        disposables.add(repository.getSettings(url)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .doFinally { _progress.postValue(false) }
            .subscribe({
                _isPublic.postValue(true)
            }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
                _isPublic.postValue(false)

            })
    }

    fun checkServer(server: UWaziUploadServer) {
        disposables.add(repository.login(server)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .doFinally { _progress.postValue(false) }
            .subscribe({ result ->
                if (result.status == TWO_FACTOR_AUTHENTICATION_CODE) {
                    _twoFactorAuthentication.postValue(true)
                } else if (result.status == AUTHENTICATION_FAILED) {
                    _authenticationError.postValue(true)
                } else {
                    if (result.isSuccess) {
                        server.isChecked = true
                        server.connectCookie = result.cookies
                        _authenticationSuccess.postValue(true)
                    }
                }
            }) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
            })
    }

    fun getSettings(server: UWaziUploadServer) {
        disposables.add(repository.getFullSettings(server)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { _progress.postValue(true) }
            .doFinally { _progress.postValue(false) }
            .subscribe({
                val list = mutableListOf<ViewLanguageItem>()
                it.languages.map { language ->
                    list.add(language.toViewLanguageItem { onLanguageClicked(language) })
                }
                _settings.postValue(Pair(it.serverName, list))
            }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )
            })
    }

    private fun onLanguageClicked(language: Language) {
        _languageClicked.postValue(language)
    }

}