package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;


public class RemoveFormFromBlankFormListEvent implements IEvent {
    private CollectForm form;

    public RemoveFormFromBlankFormListEvent(CollectForm form) {
        this.form = form;
    }

    public CollectForm getForm() {
        return form;
    }
}
