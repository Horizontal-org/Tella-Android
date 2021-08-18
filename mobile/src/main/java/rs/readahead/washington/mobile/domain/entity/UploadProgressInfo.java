package rs.readahead.washington.mobile.domain.entity;

import com.hzontal.tella_vault.VaultFile;

public class UploadProgressInfo {
    public String name;
    public long current;
    public long size;
    public Status status;
    public String fileId;

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

    public UploadProgressInfo(VaultFile file, long current, long size) {
        this.name = file.name;
        this.fileId = file.id;
        this.current = current;
        this.size = size;
        this.status = Status.OK;
    }

    public UploadProgressInfo(VaultFile file, long current, Status status) {
        this.name = file.name;
        this.fileId = file.id;
        this.current = current;
        this.size = file.size;
        this.status = status;
    }
}
