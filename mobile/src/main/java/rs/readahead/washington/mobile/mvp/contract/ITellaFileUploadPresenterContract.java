package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;

public class ITellaFileUploadPresenterContract {
    public interface IView {
        void onGetFileUploadInstancesSuccess(List<FileUploadInstance> instances);
        void onGetFileUploadInstancesError(Throwable error);
        void onFileUploadInstancesDeleted();
        void onFileUploadInstancesDeletionError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFileUploadInstances();
        void deleteFileUploadInstances(ITellaUploadsRepository.UploadStatus status);
    }
}
