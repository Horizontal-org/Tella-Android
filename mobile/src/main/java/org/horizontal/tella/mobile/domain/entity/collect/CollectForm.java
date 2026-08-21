package org.horizontal.tella.mobile.domain.entity.collect;


public class CollectForm {
    private long id;
    private long serverId;
    private String serverName;
    private String username;
    private OdkForm form;
    private boolean downloaded;
    private boolean favorite;
    private boolean updated;
    // todo: add FormDef?


    public CollectForm(long serverId, OdkForm xform) {
        this.serverId = serverId;
        this.form = xform;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public OdkForm getForm() {
        return form;
    }

    public void setForm(OdkForm form) {
        this.form = form;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean isPinned() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
