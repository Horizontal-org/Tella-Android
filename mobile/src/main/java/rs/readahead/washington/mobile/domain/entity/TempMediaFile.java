package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;
import java.util.UUID;

import rs.readahead.washington.mobile.util.C;


public final class TempMediaFile extends RawFile implements Serializable {
    public static TempMediaFile newMp4() {
        String uid = UUID.randomUUID().toString();
        return new TempMediaFile(C.TMP_DIR, uid, uid + ".mp4");
    }

    private TempMediaFile(String path, String uid, String filename) {
        this.uid = uid;
        this.path = path;
        this.fileName = filename;
    }

    @SuppressWarnings("unused")
    private TempMediaFile() {
    }
}
