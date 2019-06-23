package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;


public class ShowFormInstanceEntryEvent implements IEvent {
    private long instanceId;

    public ShowFormInstanceEntryEvent(long instanceId) {
        this.instanceId = instanceId;
    }

    public long getInstanceId() {
        return instanceId;
    }
}
