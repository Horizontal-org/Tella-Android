package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.Nullable;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;
import rs.readahead.washington.mobile.media.MediaFileBundle;


public class IQuestionAttachmentPresenterContract {
    public interface IView {
        void onGetFilesStart();
        void onGetFilesEnd();
        void onGetFilesSuccess(List<MediaFile> files);
        void onGetFilesError(Throwable error);
        void onMediaFileAdded(MediaFile mediaFile);
        void onMediaFileAddError(Throwable error);
        void onMediaFileImported(MediaFileBundle mediaFileBundle);
        void onImportError(Throwable error);
        void onImportStarted();
        void onImportEnded();
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFiles(IMediaFileRecordRepository.Filter filter, IMediaFileRecordRepository.Sort sort);
        void setAttachment(@Nullable MediaFile attachment);
        MediaFile getAttachment();
        void addNewMediaFile(MediaFileBundle mediaFileBundle);
        void addRegisteredMediaFile(long id);
        void importImage(Uri uri);
        void importVideo(Uri uri);
    }
}