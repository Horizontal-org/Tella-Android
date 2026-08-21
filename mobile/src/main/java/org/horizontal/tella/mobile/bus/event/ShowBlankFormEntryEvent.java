package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;
import org.horizontal.tella.mobile.domain.entity.collect.FormPair;


public class ShowBlankFormEntryEvent implements IEvent {
    private FormPair form;

    public ShowBlankFormEntryEvent(FormPair form) {
        this.form = form;
    }

    public FormPair getForm() {
        return form;
    }
}
