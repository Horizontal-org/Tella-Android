package rs.readahead.washington.mobile.javarosa;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;


public interface IFormReSubmitterContract {
    interface IView {
        void formReSubmitError(Throwable error);
        void formResubmitOfflineMode();
        void formReSubmitNoConnectivity();
        void formPartResubmitStart(CollectFormInstance instance, String partName);
        void formPartUploadProgress(String partName, float pct);
        void formPartResubmitSuccess(CollectFormInstance instance, OpenRosaPartResponse response);
        void formPartReSubmitError(Throwable error);
        void formPartsResubmitEnded(CollectFormInstance instance);
        void showReFormSubmitLoading(CollectFormInstance instance);
        void hideReFormSubmitLoading();
        void submissionStoppedByUser();
        Context getContext();
    }

    interface IFormReSubmitter extends IBasePresenter {
        void reSubmitFormInstanceGranular(final CollectFormInstance instance);
        void userStopReSubmission();
        void stopReSubmission();
        boolean isReSubmitting();
    }
}
