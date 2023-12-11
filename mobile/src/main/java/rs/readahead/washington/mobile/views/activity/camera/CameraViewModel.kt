package rs.readahead.washington.mobile.views.activity.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.media.MediaFileHandler
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {
    private val disposables = CompositeDisposable()
    private var currentRotation = 0

    // LiveData for UI updates
    private val _addingInProgress = SingleLiveEvent<Boolean>()
    val addingInProgress: LiveData<Boolean> = _addingInProgress

    private val _addSuccess = SingleLiveEvent<VaultFile>()
    val addSuccess: LiveData<VaultFile> = _addSuccess

    private val _addError = SingleLiveEvent<Throwable>()
    val addError: LiveData<Throwable> = _addError

    private val _rotationUpdate = SingleLiveEvent<Int>()
    val rotationUpdate: LiveData<Int> = _rotationUpdate

    private val _lastMediaFileSuccess = SingleLiveEvent<VaultFile>()
    val lastMediaFileSuccess: LiveData<VaultFile> = _lastMediaFileSuccess

    private val _lastMediaFileError = SingleLiveEvent<Throwable>()
    val lastMediaFileError: LiveData<Throwable> = _lastMediaFileError

    fun addJpegPhoto(jpeg: ByteArray, parent: String?) {
        disposables.add(
            Observable.fromCallable { MediaFileHandler.saveJpegPhoto(jpeg, parent) }
                .doOnSubscribe { _addingInProgress.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _addingInProgress.postValue(false) }
                .subscribe(
                    { bundle -> _addSuccess.postValue(bundle.blockingGet()) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _addError.postValue(throwable)
                    }
                )
        )
    }

    fun addMp4Video(file: File, parent: String?) {
        disposables.add(
            Observable.fromCallable { MediaFileHandler.saveMp4Video(file, parent) }
                .subscribeOn(Schedulers.io())
                .doOnSubscribe { _addingInProgress.postValue(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _addingInProgress.postValue(false) }
                .subscribe(
                    { vaultFile -> _addSuccess.postValue(vaultFile) },
                    { throwable ->
                        FirebaseCrashlytics.getInstance().recordException(throwable)
                        _addError.postValue(throwable)
                    }
                )
        )
    }

    fun getLastMediaFile() {
        disposables.add(
            MediaFileHandler.getLastVaultFileFromDb()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { vaultFiles -> _lastMediaFileSuccess.postValue(vaultFiles[0]) },
                    { throwable -> _lastMediaFileError.postValue(throwable) }
                )
        )
    }

    fun handleRotation(orientation: Int) {
        var degrees = 270

        if (orientation < 45 || orientation > 315) {
            degrees = 0
        } else if (orientation < 135) {
            degrees = 90
        } else if (orientation < 225) {
            degrees = 180
        }

        var rotation = (360 - degrees) % 360

        if (rotation == 270) {
            rotation = -90
        }

        if (currentRotation == rotation || rotation == 180 /*IGNORING THIS ANGLE*/) {
            return
        }

        currentRotation = rotation

        _rotationUpdate.postValue(rotation)
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }

}