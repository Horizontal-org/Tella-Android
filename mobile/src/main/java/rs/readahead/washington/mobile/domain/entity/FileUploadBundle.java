package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;

public class FileUploadBundle implements Serializable {

    private MediaFile mediaFile;
    private long serverId;
    private boolean includeMetadata;
    private boolean manualUpload;

    public FileUploadBundle(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public boolean isIncludeMetdata() {
        return includeMetadata;
    }

    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    public boolean isManualUpload() {
        return manualUpload;
    }

    public void setManualUpload(boolean manualUpload) {
        this.manualUpload = manualUpload;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }
}
