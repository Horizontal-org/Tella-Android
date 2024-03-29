package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import com.hzontal.tella_vault.VaultFile;


public class IAudioCapturePresenterContract {
    public interface IView {
        void onAddingStart();
        void onAddingEnd();
        void onAddSuccess(VaultFile mediaFile);
        void onAddError(Throwable error);
        void onAvailableStorage(long memory);
        void onAvailableStorageFailed(Throwable throwable);
        void onRecordingStopped(VaultFile vaultFile);
        void onRecordingError();
        void onDurationUpdate(long duration);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void addMediaFile(VaultFile vaultFile);
        void checkAvailableStorage();
        void stopRecorder();
        void pauseRecorder();
        void cancelRecorder();
        void cancelPauseRecorder();
        boolean isAudioRecorder();
        void startRecording(String filename, String parent);
    }
}
