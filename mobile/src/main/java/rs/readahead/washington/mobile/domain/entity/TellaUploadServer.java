package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;


public class TellaUploadServer extends Server implements Serializable {
    public static final TellaUploadServer NONE = new TellaUploadServer();


    public TellaUploadServer() {
        this(0);
    }

    public TellaUploadServer(long id) {
        setServerType(ServerType.TELLA_UPLOAD);
        setId(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (! (obj instanceof TellaUploadServer)) {
            return false;
        }

        final TellaUploadServer that = (TellaUploadServer) obj;

        return this.getId() == that.getId();
    }
}
