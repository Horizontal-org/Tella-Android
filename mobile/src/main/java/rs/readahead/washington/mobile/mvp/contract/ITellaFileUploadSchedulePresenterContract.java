package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.domain.entity.RawFile;

public class ITellaFileUploadSchedulePresenterContract {
    public interface IView {
        Context getContext();
        void onMediaFilesUploadScheduled();
        void onMediaFilesUploadScheduleError(Throwable throwable);
        void onGetMediaFilesSuccess(List<RawFile> mediaFiles);
        void onGetMediaFilesError(Throwable error);
    }

    public interface IPresenter extends IBasePresenter {
        void scheduleUploadMediaFiles(List<MediaFile> files);
        void scheduleUploadMediaFilesWithPriority(List<MediaFile> files, long uploadServerId, boolean metadata);
        void getMediaFiles(final long[] ids, boolean metadata);
    }
}