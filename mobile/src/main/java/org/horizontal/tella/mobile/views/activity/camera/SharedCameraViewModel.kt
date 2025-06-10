package org.horizontal.tella.mobile.views.activity.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.bus.event.RecentBackgroundActivitiesEvent
import org.horizontal.tella.mobile.domain.entity.background_activity.BackgroundActivityModel
import org.horizontal.tella.mobile.domain.entity.background_activity.BackgroundActivityStatus
import org.horizontal.tella.mobile.media.MediaFileHandler
import java.io.File
import javax.inject.Inject


@HiltViewModel
class SharedCameraViewModel @Inject constructor() : ViewModel() {

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
        val savePhotoSingle = try {
            MediaFileHandler.saveJpegPhoto(jpeg, parent)
        } catch (e: Exception) {
            Single.error(e)
        }

        disposables.add(savePhotoSingle
            .doOnSubscribe {
                val backgroundVideoFile = BackgroundActivityModel(
                    id = "",
                    name = "",
                    mimeType = "jpeg",
                    status = BackgroundActivityStatus.IN_PROGRESS,
                    thumb = null
                )
                _addingInProgress.postValue(true)
                MyApplication.bus().post(RecentBackgroundActivitiesEvent(mutableListOf(backgroundVideoFile)))
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { _addingInProgress.postValue(false) }
            .subscribe({ vaultFile ->
                handleAddSuccess(vaultFile, BackgroundActivityStatus.COMPLETED)
                _addSuccess.postValue(vaultFile)
            }, { throwable ->
                handleAddError(throwable)
            })
        )
    }


    fun addMp4Video(file: File, parent: String?) {
        disposables.add(Observable.fromCallable { MediaFileHandler.saveMp4Video(file, parent) }
            .subscribeOn(Schedulers.io()).doOnSubscribe {
                val backgroundVideoFile = BackgroundActivityModel(
                    id = file.name,
                    name = file.name,
                    mimeType = file.extension,
                    status = BackgroundActivityStatus.IN_PROGRESS,
                    thumb = null
                )
                _addingInProgress.postValue(true)

                // Emitting the initial status to the event bus
                MyApplication.bus()
                    .post(RecentBackgroundActivitiesEvent(mutableListOf(backgroundVideoFile)))
            }.doOnNext { vaultFile ->
                handleAddSuccess(vaultFile, BackgroundActivityStatus.IN_PROGRESS)
            }.observeOn(AndroidSchedulers.mainThread()).doFinally {
                _addingInProgress.postValue(false)
            }.subscribe({ vaultFile ->
                handleAddSuccess(vaultFile, BackgroundActivityStatus.COMPLETED)
                _addSuccess.postValue(vaultFile)
            }, { throwable ->
                handleAddError(throwable)
            }))
    }

    private fun handleAddSuccess(vaultFile: VaultFile, status: BackgroundActivityStatus) {

        val completedActivity = BackgroundActivityModel(
            id = vaultFile.id,
            name = vaultFile.name,
            mimeType = vaultFile.mimeType,
            status = status,
            thumb = vaultFile.thumb
        )
        MyApplication.bus().post(RecentBackgroundActivitiesEvent(mutableListOf(completedActivity)))
    }

    private fun handleAddError(throwable: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
        _addError.postValue(throwable)
    }

    fun getLastMediaFile() {
        disposables.add(
            MediaFileHandler.getLastVaultFileFromDb().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ vaultFiles ->   if (!vaultFiles.isNullOrEmpty()) {
                    _lastMediaFileSuccess.postValue(vaultFiles[0])
                } else {
                    _lastMediaFileError.postValue(Throwable("No media files found"))
                } },
                    { throwable -> _lastMediaFileError.postValue(throwable) })
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


}