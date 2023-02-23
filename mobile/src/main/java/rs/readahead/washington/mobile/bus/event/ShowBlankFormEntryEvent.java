package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.collect.FormPair;


public class ShowBlankFormEntryEvent implements IEvent {
    private FormPair form;

    public ShowBlankFormEntryEvent(FormPair form) {
        this.form = form;
    }

    public FormPair getForm() {
        return form;
    }
}
