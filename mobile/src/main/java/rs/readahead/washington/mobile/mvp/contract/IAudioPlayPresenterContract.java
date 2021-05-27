package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import com.hzontal.tella_vault.VaultFile;


public class IAudioPlayPresenterContract {
    public interface IView {
        void onMediaFileSuccess(VaultFile vaultFile);
        void onMediaFileError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getMediaFile(String ids);
    }
}
