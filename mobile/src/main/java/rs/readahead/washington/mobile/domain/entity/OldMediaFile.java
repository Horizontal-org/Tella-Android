package rs.readahead.washington.mobile.domain.entity;


import androidx.annotation.NonNull;
import com.hzontal.tella_vault.Metadata;

import java.io.Serializable;
import java.util.UUID;

import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.FileUtil;

public class OldMediaFile extends RawFile implements Serializable {
    public static final OldMediaFile NONE = new OldMediaFile(-1);

    public enum Type {
        UNKNOWN,
        IMAGE,
        AUDIO,
        VIDEO
    }

    private Metadata metadata;
    private long duration; // milliseconds
    private Type type;

    private String hash;

    public static OldMediaFile newPng() {
        String uid = UUID.randomUUID().toString();
        return new OldMediaFile(C.MEDIA_DIR, uid, uid + ".png", Type.IMAGE);
    }

    public static OldMediaFile newJpeg() {
        String uid = UUID.randomUUID().toString();
        return new OldMediaFile(C.MEDIA_DIR, uid, uid + ".jpg", Type.IMAGE);
    }

    public static OldMediaFile newAac() {
        String uid = UUID.randomUUID().toString();
        return new OldMediaFile(C.MEDIA_DIR, uid, uid + ".aac", Type.AUDIO);
    }

    public static OldMediaFile newMp4() {
        String uid = UUID.randomUUID().toString();
        return new OldMediaFile(C.MEDIA_DIR, uid, uid + ".mp4", Type.VIDEO);
    }

    public static OldMediaFile fromFilename(@NonNull String filename) {
        return new OldMediaFile(
                C.MEDIA_DIR,
                FileUtil.getBaseName(filename),
                filename,
                FileUtil.getOldMediaFileType(filename)
        );
    }

    // todo: this should be private and DataSource should load type from storage
    public OldMediaFile(String path, String uid, String filename, Type type) {
        this.uid = uid;
        this.path = path;
        this.fileName = filename;
        this.type = type;
    }

    private OldMediaFile() {}

    private OldMediaFile(long id) {
        setId(id);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Type getType() {
        return type;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
