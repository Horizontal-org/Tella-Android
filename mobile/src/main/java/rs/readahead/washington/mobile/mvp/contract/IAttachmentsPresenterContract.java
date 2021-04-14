package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import com.hzontal.tella_vault.VaultFile;

import java.util.List;

import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileBundle;


public class IAttachmentsPresenterContract {
    public interface IView {
        void onGetFilesStart();
        void onGetFilesEnd();
        void onGetFilesSuccess(List<VaultFile> files);
        void onGetFilesError(Throwable error);
        void onEvidenceAttached(VaultFile vaultFile);
        void onEvidenceAttachedError(Throwable error);
        void onEvidenceImported(MediaFileBundle mediaFileBundle);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFiles(IMediaFileRecordRepository.Filter filter, IMediaFileRecordRepository.Sort sort);
        void setAttachments(List<VaultFile> attachments);
        List<VaultFile> getAttachments();
        void attachNewEvidence(MediaFileBundle mediaFileBundle);
        void attachRegisteredEvidence(long id);
        void importImage(Uri uri);
        void importVideo(Uri uri);
    }
}