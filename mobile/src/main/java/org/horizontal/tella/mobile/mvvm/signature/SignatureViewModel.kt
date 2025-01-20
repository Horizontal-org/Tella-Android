package org.horizontal.tella.mobile.mvvm.signature

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.media.MediaFileHandler
import javax.inject.Inject

@HiltViewModel
class SignatureViewModel  @Inject constructor(): ViewModel() {
    private val disposables = CompositeDisposable()

    private val _addSuccess = SingleLiveEvent<VaultFile>()
    val addSuccess: LiveData<VaultFile> get() = _addSuccess

    private val _addingInProgress = SingleLiveEvent<Boolean>()
    val addingInProgress: LiveData<Boolean> get() = _addingInProgress

    private val _addError = SingleLiveEvent<Throwable>()
    val addError: LiveData<Throwable> get() = _addError

    fun addPngImage(png: ByteArray) {
        disposables.add(
            Observable.fromCallable { MediaFileHandler.savePngImage(png) }
                .subscribeOn(Schedulers.io())
                .doOnSubscribe { _addingInProgress.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _addingInProgress.postValue(false) }
                .subscribe({ mediaFile ->
                    _addSuccess.postValue(mediaFile)
                }, { throwable ->
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                    _addError.postValue(throwable)
                })
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}
