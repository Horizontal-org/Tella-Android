package rs.readahead.washington.mobile.mvp.contract;


import android.content.Context;

public class IServersPresenterContract {
    public interface IView {
        Context getContext();
        void onServersDeleted();
        void onServersDeletedError(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void deleteServers();
        void removeAutoUploadServersSettings();
        long getAutoUploadServerId();
        void setAutoUploadServerId(long id);
    }
}
