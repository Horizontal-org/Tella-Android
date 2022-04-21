package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.hzontal.tella_vault.VaultFile;


public class IMediaFileViewerPresenterContract {
    public interface IView {
        void onMediaExported();
        void onExportError(Throwable error);
        void onExportStarted();
        void onExportEnded();
        void onMediaFileDeleted();
        void onMediaFileDeletionError(Throwable throwable);
        void onMediaFileRename(VaultFile vaultFile);
        void onMediaFileRenameError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void exportNewMediaFile(VaultFile vaultFile,@Nullable Uri path);
        void deleteMediaFiles(VaultFile vaultFile);
        void renameVaultFile(String id, String name);

    }
}
