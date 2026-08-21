package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;


public class CancelPendingFormInstanceEvent implements IEvent {
    private long instanceId;

    public CancelPendingFormInstanceEvent(long instanceId) {
        this.instanceId = instanceId;
    }

    public long getInstanceId() {
        return instanceId;
    }
}
