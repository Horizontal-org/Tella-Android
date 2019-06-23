package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;


public class CollectFormInstanceDeletedEvent implements IEvent {
    private final boolean formInstanceCloned;


    public CollectFormInstanceDeletedEvent(boolean cloned) {
        this.formInstanceCloned = cloned;
    }

    public boolean isFormInstanceCloned() {
        return formInstanceCloned;
    }
}
