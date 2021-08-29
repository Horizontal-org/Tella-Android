package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import com.hzontal.tella_vault.filter.Filter;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.filter.FilterType;
import com.hzontal.tella_vault.filter.Sort;

import java.util.List;


public class IGalleryPresenterContract {
    public interface IView {
        void onGetFilesStart();
        void onGetFilesEnd();
        void onGetFilesSuccess(List<VaultFile> files);
        void onGetFilesError(Throwable error);
        void onMediaImported(VaultFile vaultFile);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        void onMediaFilesAdded(VaultFile vaultFile);
        void onMediaFilesAddingError(Throwable error);
        void onMediaFilesDeleted(int num);
        void onMediaFilesDeletionError(Throwable throwable);
        void onMediaExported(int num);
        void onExportError(Throwable error);
        void onExportStarted();
        void onExportEnded();
        void onCountTUServersEnded(Long num);
        void onCountTUServersFailed(Throwable throwable);
        //void onTmpVideoEncrypted(MediaFileBundle mediaFileBundle);
        //void onTmpVideoEncryptionError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFiles(final FilterType filterType, final Sort sort);
        void importImage(Uri uri);
        void importVideo(Uri uri);
        void addNewMediaFile(VaultFile vaultFile);
        void deleteMediaFiles(List<VaultFile> mediaFiles);
        void exportMediaFiles(List<VaultFile> mediaFiles);
        void countTUServers();
        //void encryptTmpVideo(Uri uri);
    }
}
