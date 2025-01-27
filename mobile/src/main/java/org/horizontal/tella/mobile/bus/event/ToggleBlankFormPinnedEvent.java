package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm;


public class ToggleBlankFormPinnedEvent implements IEvent {
    private CollectForm form;

    public ToggleBlankFormPinnedEvent(CollectForm form) {
        this.form = form;
    }

    public CollectForm getForm() {
        return form;
    }
}
