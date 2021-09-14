package rs.readahead.washington.mobile.mvp.presenter;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import com.hzontal.tella_vault.VaultFile;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.media.AudioRecorder;
import rs.readahead.washington.mobile.mvp.contract.IAudioCapturePresenterContract;


public class AudioCapturePresenter implements IAudioCapturePresenterContract.IPresenter, AudioRecorder.AudioRecordInterface {
    private IAudioCapturePresenterContract.IView view;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private AudioRecorder audioRecorder = null;

    public AudioCapturePresenter(IAudioCapturePresenterContract.IView view) {
        this.view = view;
    }

    @Override
    public void addMediaFile(VaultFile vaultFile) { // audio recorder creates MediaFile's file already encrypted and in place
    }

    @Override
    public void checkAvailableStorage() {
        disposables.add(Single.fromCallable(() -> {
                    StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
                    long freeSpace;

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        freeSpace = ((long) statFs.getAvailableBlocks() * statFs.getBlockSize());
                    } else {
                        freeSpace = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());
                    }

                    return freeSpace;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(free -> view.onAvailableStorage(free),
                        throwable -> view.onAvailableStorageFailed(throwable))
        );
    }

    @Override
    public boolean isAudioRecorder() {
        return (audioRecorder == null);
    }

    @Override
    public void startRecording(String filename) {
        audioRecorder = new AudioRecorder(this);
        disposables.add(audioRecorder.startRecording(filename)
                .subscribe(vaultFile -> view.onRecordingStopped(vaultFile), throwable -> view.onRecordingError())
        );
    }

    @Override
    public void stopRecorder() {
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
            audioRecorder = null;
        }
    }

    @Override
    public void pauseRecorder() {
        if (audioRecorder != null) {
            audioRecorder.pauseRecording();
        }
    }

    @Override
    public void cancelPauseRecorder() {
        if (audioRecorder != null) {
            audioRecorder.cancelPause();
        }
    }

    @Override
    public void cancelRecorder() {
        if (audioRecorder != null) {
            audioRecorder.cancelRecording();
            audioRecorder = null;
        }
    }

    @Override
    public void destroy() {
        disposables.dispose();
        cancelRecorder();
        view = null;
    }

    @Override
    public void onDurationUpdate(long duration) {
        view.onDurationUpdate(duration);
    }
}
