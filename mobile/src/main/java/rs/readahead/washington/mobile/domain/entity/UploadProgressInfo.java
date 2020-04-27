package rs.readahead.washington.mobile.domain.entity;

public class UploadProgressInfo {
    public String name;
    public long current;
    public long size;
    public Status status;
    public long fileId;

    public enum Status {
        UNKNOWN,
        STARTED,
        OK,
        FINISHED,
        ERROR,
        UNAUTHORIZED,
        CONFLICT,
        UNKNOWN_HOST,
    }

    public UploadProgressInfo(RawFile file, long current, long size) {
        this.name = file.getFileName();
        this.fileId = file.getId();
        this.current = current;
        this.size = size;
        this.status = Status.OK;
    }

    public UploadProgressInfo(RawFile file, long current, Status status) {
        this.name = file.getFileName();
        this.fileId = file.getId();
        this.current = current;
        this.size = file.getSize();
        this.status = status;
    }
}
