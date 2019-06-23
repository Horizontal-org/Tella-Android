package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;


public class ToggleBlankFormPinnedEvent implements IEvent {
    private CollectForm form;

    public ToggleBlankFormPinnedEvent(CollectForm form) {
        this.form = form;
    }

    public CollectForm getForm() {
        return form;
    }
}
