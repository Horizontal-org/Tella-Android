package rs.readahead.washington.mobile.views.fragment.vault.home;

import android.content.Context;

import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;

public class IHomeVaultPresenter {

    public interface IView {
        Context getContext();
        void onCountTUServersEnded(Long num);
        void onCountTUServersFailed(Throwable throwable);
        void onCountCollectServersEnded(Long num);
        void onCountCollectServersFailed(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void executePanicMode();
        void countTUServers();
        void countCollectServers();
    }
}

