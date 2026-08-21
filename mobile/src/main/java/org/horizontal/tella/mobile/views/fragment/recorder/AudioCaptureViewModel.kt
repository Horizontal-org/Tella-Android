package org.horizontal.tella.mobile.views.fragment.recorder

import org.horizontal.tella.mobile.domain.usecases.shared.ScheduleUploadReportFilesUseCase
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.bus.SingleLiveEvent
import org.horizontal.tella.mobile.bus.event.RecentBackgroundActivitiesEvent
import org.horizontal.tella.mobile.domain.entity.background_activity.BackgroundActivityModel
import org.horizontal.tella.mobile.domain.entity.background_activity.BackgroundActivityStatus
import org.horizontal.tella.mobile.domain.usecases.shared.CheckFileNameUseCase
import org.horizontal.tella.mobile.media.AudioRecorder
import javax.inject.Inject

@HiltViewModel
class AudioCaptureViewModel @Inject constructor(
    private val scheduleUploadReportFilesUseCase: ScheduleUploadReportFilesUseCase,
    private val checkFileNameUseCase: CheckFileNameUseCase
) :
    ViewModel(),
    AudioRecorder.AudioRecordInterface {
    private val _availableStorageLiveData = SingleLiveEvent<Long>()
    val availableStorageLiveData: LiveData<Long>
        get() = _availableStorageLiveData

    private val _durationLiveData = SingleLiveEvent<Long>()
    val durationLiveData: LiveData<Long>
        get() = _durationLiveData

    private val _recordingStoppedLiveData = SingleLiveEvent<VaultFile>()
    val recordingStoppedLiveData: LiveData<VaultFile>
        get() = _recordingStoppedLiveData

    private val _recordingErrorLiveData = SingleLiveEvent<Throwable>()
    val recordingErrorLiveData: LiveData<Throwable>
        get() = _recordingErrorLiveData

    private val _mediaFilesUploadScheduled = SingleLiveEvent<Boolean>()
    val mediaFilesUploadScheduled: LiveData<Boolean>
        get() = _mediaFilesUploadScheduled

    private val _mediaFilesUploadScheduleError = MutableLiveData<Throwable>()
    val mediaFilesUploadScheduleError: LiveData<Throwable>
        get() = _mediaFilesUploadScheduleError

    private val _addingInProgress = SingleLiveEvent<Boolean>()
    val addingInProgress: LiveData<Boolean> = _addingInProgress

    private val disposables = CompositeDisposable()
    private var audioRecorder: AudioRecorder? = null


    private val _isFileNameUnique = SingleLiveEvent<Boolean>()
    val isFileNameUnique: LiveData<Boolean> = _isFileNameUnique

    fun checkFileName(fileName: String) {
        disposables.add(
            checkFileNameUseCase.isFileNameUnique(fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ isUnique ->
                    // Emit the result to LiveData
                    _isFileNameUnique.postValue(isUnique)
                }, { error ->
                    // In case of error, emit false (or handle the error accordingly)
                    _isFileNameUnique.postValue(false)
                })
        )
    }

    fun checkAvailableStorage() {
        disposables.add(Single.fromCallable {
            val statFs = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            val freeSpace: Long =
                (statFs.availableBlocksLong * statFs.blockSizeLong)
            freeSpace
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { freeSpace -> _availableStorageLiveData.value = freeSpace },
                { /* Handle error if needed */ }
            )
        )
    }

    fun isAudioRecorder(): Boolean {
        return audioRecorder == null
    }

    fun startRecording(filename: String, parent: String?) {
        audioRecorder = AudioRecorder(this)
        disposables.add(
            audioRecorder!!.startRecording(filename, parent)
                .doOnNext { vaultFile ->
                    handleAddSuccess(
                        vaultFile,
                        BackgroundActivityStatus.IN_PROGRESS
                    )
                }.doOnSubscribe {
                    _addingInProgress.postValue(true)
                    val backgroundAudioFile = BackgroundActivityModel(
                        id = filename,
                        name = filename,
                        mimeType = "mp3",
                        status = BackgroundActivityStatus.IN_PROGRESS,
                        thumb = null
                    )
                    MyApplication.bus()
                        .post(RecentBackgroundActivitiesEvent(mutableListOf(backgroundAudioFile)))
                }
                .subscribe(
                    { vaultFile: VaultFile ->

                        handleAddSuccess(vaultFile, BackgroundActivityStatus.COMPLETED)
                        _recordingStoppedLiveData.postValue(
                            vaultFile
                        )
                        _addingInProgress.postValue(false)

                    }
                ) { throwable: Throwable ->
                    _recordingErrorLiveData.postValue(throwable)
                }
        )
    }

    fun stopRecorder() {
        if (audioRecorder != null) {
            audioRecorder!!.stopRecording()
            audioRecorder = null
        }
    }

    fun pauseRecorder() {
        if (audioRecorder != null) {
            audioRecorder!!.pauseRecording()
        }
    }

    fun cancelPauseRecorder() {
        if (audioRecorder != null) {
            audioRecorder!!.cancelPause()
        }
    }

    fun cancelRecorder() {
        if (audioRecorder != null) {
            audioRecorder!!.cancelRecording()
            audioRecorder = null
        }
    }

    fun scheduleUploadReportFiles(vaultFile: VaultFile, uploadServerId: Long) {
        disposables.add(
            scheduleUploadReportFilesUseCase.execute(vaultFile, uploadServerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    _mediaFilesUploadScheduled.postValue(true)
                }, { throwable ->
                    _mediaFilesUploadScheduleError.postValue(throwable)
                })
        )
    }

    private fun handleAddSuccess(vaultFile: VaultFile, status: BackgroundActivityStatus) {

        val backgroundAudioFile = BackgroundActivityModel(
            id = vaultFile.id,
            name = vaultFile.name,
            mimeType = vaultFile.mimeType,
            status = status,
            thumb = vaultFile.thumb
        )
        MyApplication.bus()
            .post(RecentBackgroundActivitiesEvent(mutableListOf(backgroundAudioFile)))
    }


    override fun onCleared() {
        super.onCleared()
        cancelRecorder()
    }

    override fun onDurationUpdate(duration: Long) {
        _durationLiveData.postValue(duration)
    }

}