package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class IAudioCapturePresenterContract {
    public interface IView {
        void onAddingStart();
        void onAddingEnd();
        void onAddSuccess(long mediaFileId);
        void onAddError(Throwable error);
        void onAvailableStorage(long memory);
        void onAvailableStorageFailed(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void addMediaFile(MediaFile mediaFile);
        void checkAvailableStorage();
    }
}
