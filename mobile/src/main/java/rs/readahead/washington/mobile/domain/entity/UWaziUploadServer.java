package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;

public class UWaziUploadServer extends Server implements Serializable {
    public static final UWaziUploadServer NONE = new UWaziUploadServer();


    public UWaziUploadServer() {
        this(0);
    }

    public UWaziUploadServer(long id) {
        setServerType(ServerType.UWAZI);
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
