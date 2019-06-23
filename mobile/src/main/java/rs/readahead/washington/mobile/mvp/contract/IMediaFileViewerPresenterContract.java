package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class IMediaFileViewerPresenterContract {
    public interface IView {
        void onMediaExported();
        void onExportError(Throwable error);
        void onExportStarted();
        void onExportEnded();
        void onMediaFileDeleted();
        void onMediaFileDeletionError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void exportNewMediaFile(MediaFile mediaFile);
        void deleteMediaFiles(MediaFile mediaFiles);
    }
}
