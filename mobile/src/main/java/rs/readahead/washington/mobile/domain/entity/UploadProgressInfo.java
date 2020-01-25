package rs.readahead.washington.mobile.domain.entity;

public class UploadProgressInfo {
    public String name;
    public long current;
    public long size;
    public Status status;

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

    public UploadProgressInfo(String name, long current, long size) {
        this(name, current, size, Status.OK);
    }

    public UploadProgressInfo(String name, long current, long size, Status status) {
        this.name = name;
        this.current = current;
        this.size = size;
        this.status = status;
    }
}
