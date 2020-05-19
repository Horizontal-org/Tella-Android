package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;

import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository.UploadStatus;

public class FileUploadInstance implements Serializable {
    private long id;
    private MediaFile mediaFile;
    private long updated;
    private UploadStatus status;
    private long size;
    private long uploaded;
    private int retryCount;
    private long set;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getUploaded() {
        return uploaded;
    }

    public void setUploaded(long uploaded) {
        this.uploaded = uploaded;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getSet() {
        return set;
    }

    public void setSet(long set) {
        this.set = set;
    }
}
