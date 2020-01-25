package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

public class IProtectionSettingsPresenterContract {
    public interface IView {
        Context getContext();
        void onCountCollectServersEnded(long num);
        void onCountCollectServersFailed(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void countCollectServers();
    }
}
