package org.horizontal.tella.mobile.views.interfaces;

import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance;

public interface ISavedFormsInterface {

    void showFormsMenu(CollectFormInstance instance);
    void showFormInstance(CollectFormInstance instance);
    void reSubmitForm(CollectFormInstance instance);
}
