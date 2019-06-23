package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;


public class DeleteFormInstanceEvent implements IEvent {
    private long instanceId;
    private CollectFormInstanceStatus status;

    public DeleteFormInstanceEvent(long instanceId, CollectFormInstanceStatus status) {
        this.instanceId = instanceId;
        this.status = status;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public CollectFormInstanceStatus getStatus() {
        return status;
    }
}
