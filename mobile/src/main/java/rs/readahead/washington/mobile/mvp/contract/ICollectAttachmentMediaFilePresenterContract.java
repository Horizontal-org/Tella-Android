package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.MediaFile;

public class ICollectAttachmentMediaFilePresenterContract {
    public interface IView {
        void onGetMediaFileSuccess(MediaFile mediaFile);
        void onGetMediaFileStart();
        void onGetMediaFileEnd();
        void onGetMediaFileError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getMediaFile(String fileName);
    }
}
