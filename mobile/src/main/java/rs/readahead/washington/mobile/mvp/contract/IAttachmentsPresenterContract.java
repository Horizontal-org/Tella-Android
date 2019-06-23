package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileBundle;


public class IAttachmentsPresenterContract {
    public interface IView {
        void onGetFilesStart();
        void onGetFilesEnd();
        void onGetFilesSuccess(List<MediaFile> files);
        void onGetFilesError(Throwable error);
        void onEvidenceAttached(MediaFile mediaFile);
        void onEvidenceAttachedError(Throwable error);
        void onEvidenceImported(MediaFileBundle mediaFileBundle);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFiles(IMediaFileRecordRepository.Filter filter, IMediaFileRecordRepository.Sort sort);
        void setAttachments(List<MediaFile> attachments);
        List<MediaFile> getAttachments();
        void attachNewEvidence(MediaFileBundle mediaFileBundle);
        void attachRegisteredEvidence(long id);
        void importImage(Uri uri);
        void importVideo(Uri uri);
    }
}