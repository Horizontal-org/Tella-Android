package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.RawFile;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;

public class IFileUploadingPresenterContract {
    public interface IView {
        void onMediaFilesUploadStarted();
        void onMediaFilesUploadProgress(UploadProgressInfo progressInfo);
        void onMediaFilesUploadEnded();
        void onGetMediaFilesSuccess(List<RawFile> mediaFiles);
        void onGetMediaFilesError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getMediaFiles(final long[] ids, boolean metadata);
        void uploadMediaFiles(TellaUploadServer server, List<RawFile> mediaFiles, boolean metadata);
        void stopUploading();
    }
}
