package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;


public class ShowFormInstanceEntryEvent implements IEvent {
    private long instanceId;

    public ShowFormInstanceEntryEvent(long instanceId) {
        this.instanceId = instanceId;
    }

    public long getInstanceId() {
        return instanceId;
    }
}
