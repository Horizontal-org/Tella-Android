package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import java.util.List;

import rs.readahead.washington.mobile.domain.entity.FileUploadInstance;

public class ITellaFileUploadPresenterContract {
    public interface IView {
        void onGetFileUploadInstancesSuccess(List<FileUploadInstance> instances);
        void onGetFileUploadInstancesError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void getFileUploadInstances();
    }
}
