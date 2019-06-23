package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.MediaFile;

public class ISignaturePresenterContract {
    public interface IView {
        void onAddingStart();
        void onAddingEnd();
        void onAddSuccess(MediaFile mediafile);
        void onAddError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void addPngImage(byte[] png);
    }
}
