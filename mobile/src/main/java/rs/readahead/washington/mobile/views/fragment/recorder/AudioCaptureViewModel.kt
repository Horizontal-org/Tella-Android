package rs.readahead.washington.mobile.views.fragment.recorder

import rs.readahead.washington.mobile.domain.usecases.shared.ScheduleUploadReportFilesUseCase
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
import rs.readahead.washington.mobile.bus.SingleLiveEvent
import rs.readahead.washington.mobile.media.AudioRecorder
import javax.inject.Inject

@HiltViewModel
class AudioCaptureViewModel @Inject constructor(private val scheduleUploadReportFilesUseCase: ScheduleUploadReportFilesUseCase) : ViewModel(),
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


    // LiveData to communicate with the view
    private val _mediaFilesUploadScheduled = SingleLiveEvent<Boolean>()
    val mediaFilesUploadScheduled: LiveData<Boolean>
        get() = _mediaFilesUploadScheduled

    private val _mediaFilesUploadScheduleError = MutableLiveData<Throwable>()
    val mediaFilesUploadScheduleError: LiveData<Throwable>
        get() = _mediaFilesUploadScheduleError

    private val disposables = CompositeDisposable()
    private var audioRecorder: AudioRecorder? = null


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

    fun startRecording(filename: String?, parent: String?) {
        audioRecorder = AudioRecorder(this)
        disposables.add(
            audioRecorder!!.startRecording(filename, parent)
                .subscribe(
                    { vaultFile: VaultFile? ->
                        _recordingStoppedLiveData.postValue(
                            vaultFile
                        )
                    }
                ) { throwable: Throwable? -> _recordingErrorLiveData.postValue(throwable) }
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


    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
        cancelRecorder()
    }

    override fun onDurationUpdate(duration: Long) {
        _durationLiveData.postValue(duration)
    }

}