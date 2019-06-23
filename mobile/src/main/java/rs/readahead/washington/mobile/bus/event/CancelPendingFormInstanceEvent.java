package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;


public class CancelPendingFormInstanceEvent implements IEvent {
    private long instanceId;

    public CancelPendingFormInstanceEvent(long instanceId) {
        this.instanceId = instanceId;
    }

    public long getInstanceId() {
        return instanceId;
    }
}
