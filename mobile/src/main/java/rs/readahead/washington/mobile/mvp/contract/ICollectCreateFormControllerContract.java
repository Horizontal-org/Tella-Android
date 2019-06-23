package rs.readahead.washington.mobile.mvp.contract;

import android.content.Context;

import org.javarosa.core.model.FormDef;

import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.odk.FormController;


public class ICollectCreateFormControllerContract {
    public interface IView {
        void onFormControllerCreated(FormController formController);
        void onFormControllerError(Throwable error);
        Context getContext();
    }

    public interface IPresenter extends IBasePresenter {
        void createFormController(CollectFormInstance instance);
        void createFormController(CollectForm collectForm, FormDef formDef);
    }
}
