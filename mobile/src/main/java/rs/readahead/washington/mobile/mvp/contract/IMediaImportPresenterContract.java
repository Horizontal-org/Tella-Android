package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;
import com.hzontal.tella_vault.VaultFile;


public class IMediaImportPresenterContract {
    public interface IView {
        void onMediaFileImported(VaultFile vaultFile);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void addNewMediaFile(VaultFile vaultFile);
        void setAttachment(@Nullable VaultFile attachment);
        VaultFile getAttachment();
        void addRegisteredMediaFile(long id);
        void importFile(Uri uri);
        void importImage(Uri uri);
        void importVideo(Uri uri);
    }
}
