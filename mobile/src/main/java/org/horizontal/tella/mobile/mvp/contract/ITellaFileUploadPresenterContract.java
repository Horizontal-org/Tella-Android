package org.horizontal.tella.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import org.horizontal.tella.mobile.domain.entity.FileUploadInstance;
import org.horizontal.tella.mobile.domain.repository.ITellaUploadsRepository;

public class ITellaFileUploadPresenterContract {
    public interface IView {
        void onGetFileUploadInstancesSuccess(List<FileUploadInstance> instances);
        void onGetFileUploadInstancesError(Throwable error);
        void onGetFileUploadSetInstancesSuccess(List<FileUploadInstance> instances);
        void onGetFileUploadSetInstancesError(Throwable error);
        void onFileUploadInstancesDeleted();
        void onFileUploadInstancesDeletionError(Throwable throwable);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFileUploadInstances();
        void getFileUploadSetInstances(long set);
        void deleteFileUploadInstance(long id);
        void deleteFileUploadInstances(long set);
        void deleteFileUploadInstancesInStatus(ITellaUploadsRepository.UploadStatus status);
        void deleteFileUploadInstancesNotInStatus(ITellaUploadsRepository.UploadStatus status);
    }
}
