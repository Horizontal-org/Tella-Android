package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;


public class ICheckTUSServerContract {
    public interface IView {
        void onServerCheckSuccess(TellaUploadServer server);
        void onServerCheckFailure(UploadProgressInfo.Status status);
        void onServerCheckError(Throwable error);
        void showServerCheckLoading();
        void hideServerCheckLoading();
        void onNoConnectionAvailable();
        void setSaveAnyway(boolean enabled);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void checkServer(TellaUploadServer server, boolean connectionRequired);
    }
}
