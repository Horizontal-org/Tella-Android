package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance;


public class ReSubmitFormInstanceEvent implements IEvent {
    private final CollectFormInstance instance;


    public ReSubmitFormInstanceEvent(CollectFormInstance instance) {
        this.instance = instance;
    }

    public CollectFormInstance getInstance() {
        return instance;
    }
}
