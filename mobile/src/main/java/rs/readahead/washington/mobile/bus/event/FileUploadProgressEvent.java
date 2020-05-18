package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;

public class FileUploadProgressEvent implements IEvent {
    private long progress;

    public FileUploadProgressEvent(long progress) {
        this.progress = progress;
    }

    public long getProgress() {
        return progress;
    }
}
