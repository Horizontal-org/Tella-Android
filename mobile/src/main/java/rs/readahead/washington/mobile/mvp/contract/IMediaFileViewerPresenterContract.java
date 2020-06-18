package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.repository.IMediaFileRecordRepository;


public class IMediaFileViewerPresenterContract {
    public interface IView {
        void onMediaExported();
        void onExportError(Throwable error);
        void onExportStarted();
        void onExportEnded();
        void onMediaFileDeleted();
        void onMediaFileDeletionError(Throwable throwable);
        void onGetMediaFileSuccess(MediaFile mediaFile);
        void onGetMediaFileError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void exportNewMediaFile(MediaFile mediaFile);
        void deleteMediaFiles(MediaFile mediaFiles);
        void getMediaFile(long mediaFileId, IMediaFileRecordRepository.Direction direction);
    }
}
