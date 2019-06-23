package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;


public class ReSubmitFormInstanceEvent implements IEvent {
    private final CollectFormInstance instance;


    public ReSubmitFormInstanceEvent(CollectFormInstance instance) {
        this.instance = instance;
    }

    public CollectFormInstance getInstance() {
        return instance;
    }
}
