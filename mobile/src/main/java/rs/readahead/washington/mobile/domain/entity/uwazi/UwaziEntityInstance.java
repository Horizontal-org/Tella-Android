package rs.readahead.washington.mobile.domain.entity.uwazi;

import java.io.Serializable;

public class UwaziEntityInstance implements Serializable{
    private long id = -1;
    private long updated;
    private CollectTemplate template;
    private UwaziEntityStatus status = UwaziEntityStatus.UNKNOWN;


    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public long getUpdated() {
        return updated;
    }
    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public CollectTemplate getCollectTemplate() {
        return template;
    }
    public void setCollectTemplate(CollectTemplate template) {
        this.template = template;
    }

    public UwaziEntityStatus getStatus() {
        return status;
    }
    public void setStatus(UwaziEntityStatus status) {
        this.status = status;
    }
}
