package rs.readahead.washington.mobile.domain.entity.uwazi;

import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow;

public class CollectTemplate {
    private long id ;
    private long serverId;
    private String serverName;
    private String username;
    private UwaziEntityRow entityRow;
    private boolean downloaded;
    private boolean favorite;
    private boolean updated;

    public CollectTemplate(long id, UwaziEntityRow entityRow) {
        this.id = id;
        this.entityRow = entityRow;
    }

    public CollectTemplate(long id, long serverId, String serverName, String username, UwaziEntityRow entityRow, boolean downloaded, boolean favorite, boolean updated) {
        this.id = id;
        this.serverId = serverId;
        this.serverName = serverName;
        this.username = username;
        this.entityRow = entityRow;
        this.downloaded = downloaded;
        this.favorite = favorite;
        this.updated = updated;
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

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UwaziEntityRow getEntityRow() {
        return entityRow;
    }

    public void setEntityRow(UwaziEntityRow entityRow) {
        this.entityRow = entityRow;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
