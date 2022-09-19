package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.hzontal.tella_vault.VaultFile;

import java.util.List;

import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;


public class IQuestionAttachmentPresenterContract {
    public interface IView {
        void onGetFilesStart();
        void onGetFilesEnd();
        void onGetFilesSuccess(List<VaultFile> files);
        void onGetFilesError(Throwable error);
        void onMediaFileAdded(VaultFile vaultFile);
        void onMediaFileAddError(Throwable error);
        void onMediaFileImported(VaultFile vaultFile);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFiles(IMediaFileRecordRepository.Filter filter, IMediaFileRecordRepository.Sort sort);
        void setAttachment(@Nullable VaultFile attachment);
        VaultFile getAttachment();
        void addNewMediaFile(VaultFile vaultFile);
        void addRegisteredMediaFile(long id);
        void importImage(Uri uri);
        void importVideo(Uri uri);
    }
}