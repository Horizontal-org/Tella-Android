package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;

public class ITellaFileUploadPresenterContract {
    public interface IView {
        Context getContext();
        void onMediaFilesUploadScheduled();
        void onMediaFilesUploadScheduleError(Throwable throwable);
    }

    public interface IPresenter extends IBasePresenter {
        void scheduleUploadMediaFiles(List<MediaFile> files);
    }
}