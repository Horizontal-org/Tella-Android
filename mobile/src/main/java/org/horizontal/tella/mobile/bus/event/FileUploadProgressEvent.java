package org.horizontal.tella.mobile.bus.event;

import org.horizontal.tella.mobile.bus.IEvent;
import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo;

public class FileUploadProgressEvent implements IEvent {
    private UploadProgressInfo progress;

    public FileUploadProgressEvent(UploadProgressInfo progress) {
        this.progress = progress;
    }

    public UploadProgressInfo getProgress() {
        return progress;
    }
}
