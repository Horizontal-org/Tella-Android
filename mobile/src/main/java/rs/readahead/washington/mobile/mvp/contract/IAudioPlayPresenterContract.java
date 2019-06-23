package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class IAudioPlayPresenterContract {
    public interface IView {
        void onMediaFileSuccess(MediaFile mediaFile);
        void onMediaFileError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getMediaFile(long ids);
    }
}
