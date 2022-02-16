package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;

public class UWaziUploadServer extends Server implements Serializable {
    public static final UWaziUploadServer NONE = new UWaziUploadServer();
    private String connectCookie;
    private String localeCookie = "en";
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

        if (! (obj instanceof UWaziUploadServer)) {
            return false;
        }

        final UWaziUploadServer that = (UWaziUploadServer) obj;

        return this.getId() == that.getId();
    }

    public String getConnectCookie() {
        return connectCookie;
    }

    public void setConnectCookie(String connectCookie) {
        this.connectCookie = connectCookie;
    }

    public String getLocaleCookie() {
        return localeCookie;
    }

    public void setLocaleCookie(String localeCookie) {
        this.localeCookie = localeCookie;
    }
}
