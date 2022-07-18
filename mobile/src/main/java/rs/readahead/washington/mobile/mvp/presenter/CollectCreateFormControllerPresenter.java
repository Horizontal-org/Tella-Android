package rs.readahead.washington.mobile.mvp.presenter;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;

import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.mvp.contract.ICollectCreateFormControllerContract;
import rs.readahead.washington.mobile.odk.FormController;


public class CollectCreateFormControllerPresenter implements
        ICollectCreateFormControllerContract.IPresenter {
    private ICollectCreateFormControllerContract.IView view;


    public CollectCreateFormControllerPresenter(ICollectCreateFormControllerContract.IView view) {
        this.view = view;
    }

    @Override
    public void createFormController(CollectForm collectForm, FormDef formDef) {
        try {
            CollectFormInstance instance = new CollectFormInstance();
            instance.setServerId(collectForm.getServerId());
            instance.setServerName(collectForm.getServerName());
            instance.setUsername(collectForm.getUsername());
            instance.setStatus(CollectFormInstanceStatus.UNKNOWN);
            instance.setFormID(collectForm.getForm().getFormID());
            instance.setVersion(collectForm.getForm().getVersion());
            instance.setFormName(collectForm.getForm().getName());
            instance.setInstanceName(collectForm.getForm().getName());

            FormController fc = createFormController(instance, formDef);
            view.onFormControllerCreated(fc);
        } catch (Throwable throwable) {
            view.onFormControllerError(throwable);
        }
    }

    @Override
    public void createFormController(CollectFormInstance instance) {
        try {
            FormController fc = createFormController(instance, instance.getFormDef());
            view.onFormControllerCreated(fc);
        } catch (Throwable throwable) {
            view.onFormControllerError(throwable);
        }
    }

    @Override
    public void destroy() {
        view = null;
    }

    private FormController createFormController(CollectFormInstance instance, FormDef formDef) {
        if (formDef == null) {
            throw new IllegalArgumentException();
        }

        FormEntryModel fem = new FormEntryModel(formDef);
        FormEntryController fec = new FormEntryController(fem);

        FormController fc = new FormController(fec, instance);
        FormController.setActive(fc);

        // true - no saved instance data..
        formDef.initialize(true, new InstanceInitializationFactory());

        // Remove previous forms
        //ReferenceManager.__().clearSession();
         ReferenceManager._().clearSession();

        // This should get moved to the Application Class
        /*if (ReferenceManager._().getFactories().length == 0) {
            // this is /sdcard/odk
            ReferenceManager._().addReferenceFactory(new FileReferenceFactory(Collect.ODK_ROOT));
        }*/

        fc.initFormChangeTracking(); // set clear form to track changes

        return fc;
    }
}
