package rs.readahead.washington.mobile.javarosa;

import android.content.Context;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;
import rs.readahead.washington.mobile.mvp.contract.IBasePresenter;


public interface IFormSubmitterContract {
    interface IView {
        void formSubmitError(Throwable error);
        void formPartSubmitError(Throwable error);
        void formSubmitNoConnectivity();
        //void formSubmitSuccess(CollectFormInstance instance, OpenRosaResponse response);
        void formPartSubmitStart(CollectFormInstance instance, String partName);
        void formPartUploadProgress(String partName, float pct);
        void formPartSubmitSuccess(CollectFormInstance instance, OpenRosaPartResponse response);
        void formPartsSubmitEnded(CollectFormInstance instance);
        void showFormSubmitLoading(CollectFormInstance instance);
        void hideFormSubmitLoading();
        void submissionStoppedByUser();
        void saveForLaterFormInstanceSuccess();
        void saveForLaterFormInstanceError(Throwable error);
        Context getContext();
    }

    interface IFormSubmitter extends IBasePresenter {
        void submitActiveFormInstance(String name);
        //void submitFormInstance(CollectFormInstance instance);
        void submitFormInstanceGranular(final CollectFormInstance instance);
        void saveForLaterFormInstance(String name);
        void userStopSubmission();
        void stopSubmission();
        boolean isSubmitting();
    }
}
