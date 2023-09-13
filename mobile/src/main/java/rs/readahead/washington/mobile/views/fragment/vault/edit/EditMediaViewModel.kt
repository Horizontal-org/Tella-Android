package rs.readahead.washington.mobile.views.fragment.vault.edit

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.media.MediaFileHandler
import javax.inject.Inject

@HiltViewModel
class EditMediaViewModel @Inject constructor() : ViewModel() {
    private val disposables = CompositeDisposable()

    private val _saveInProgress = SingleLiveEvent<Boolean>()
    val saveInProgress: LiveData<Boolean> = _saveInProgress

    private val _saveSuccess = SingleLiveEvent<VaultFile>()
    val saveSuccess: LiveData<VaultFile> = _saveSuccess

    private val _saveError = SingleLiveEvent<Throwable>()
    val saveError: LiveData<Throwable> = _saveError


    fun saveBitmapAsJpeg(bitmap: Bitmap, parent: String?) {
        disposables.add(
            Observable.fromCallable { MediaFileHandler.saveBitmapAsJpeg(bitmap, parent) }
                .doOnSubscribe { _saveInProgress.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _saveInProgress.postValue(false) }
                .subscribe(
                    { bundle -> _saveSuccess.postValue(bundle.blockingGet()) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _saveError.postValue(throwable)
                    }
                )
        )
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }

}
