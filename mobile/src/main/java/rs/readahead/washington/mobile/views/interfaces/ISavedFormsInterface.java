package rs.readahead.washington.mobile.views.interfaces;

import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;

public interface ISavedFormsInterface {

    void showFormsMenu(CollectFormInstance instance);
    void reSubmitForm(CollectFormInstance instance);
}
