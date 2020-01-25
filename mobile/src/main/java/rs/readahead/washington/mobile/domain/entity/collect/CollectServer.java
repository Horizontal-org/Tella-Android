package rs.readahead.washington.mobile.domain.entity.collect;

import java.io.Serializable;

import rs.readahead.washington.mobile.domain.entity.Server;
import rs.readahead.washington.mobile.domain.entity.ServerType;


public class CollectServer extends Server implements Serializable {
    public static final CollectServer NONE = new CollectServer();


    public CollectServer() {
        this(0);
    }

    public CollectServer(long id) {
        setServerType(ServerType.ODK_COLLECT);
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

        if (!(obj instanceof CollectServer)) {
            return false;
        }

        final CollectServer that = (CollectServer) obj;

        return this.getId() == that.getId();
    }
}
