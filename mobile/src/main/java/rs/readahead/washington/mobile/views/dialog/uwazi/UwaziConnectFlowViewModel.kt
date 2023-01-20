package rs.readahead.washington.mobile.views.dialog.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.database.UwaziDataSource
import rs.readahead.washington.mobile.data.entity.uwazi.LanguageEntity
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.Language
import rs.readahead.washington.mobile.views.adapters.uwazi.ViewLanguageItem
import rs.readahead.washington.mobile.views.fragment.uwazi.mappers.toViewLanguageItem
import timber.log.Timber

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
                Timber.e(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )//TODO Crahslytics removed
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
                Timber.e(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )//TODO Crahslytics removed
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
                Timber.e(
                    throwable
                        ?: throw NullPointerException("Expression 'throwable' must not be null")
                )//TODO Crahslytics removed
            })
    }

    private fun onLanguageClicked(language: Language) {
        _languageClicked.postValue(language)
    }

    fun updateLanguageSettings(server: UWaziUploadServer, language: LanguageEntity) {
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
                Timber.e(throwable!!)//TODO Crahslytics removed
                error.postValue(throwable)
            }
        )
    }

}