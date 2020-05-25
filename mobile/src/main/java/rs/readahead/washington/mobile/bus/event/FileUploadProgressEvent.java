package rs.readahead.washington.mobile.bus.event;

import rs.readahead.washington.mobile.bus.IEvent;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;

public class FileUploadProgressEvent implements IEvent {
    private UploadProgressInfo progress;

    public FileUploadProgressEvent(UploadProgressInfo progress) {
        this.progress = progress;
    }

    public UploadProgressInfo getProgress() {
        return progress;
    }
}
