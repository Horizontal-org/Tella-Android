package rs.readahead.washington.mobile.domain.entity;

import rs.readahead.washington.mobile.presentation.entity.DownloadState;


public class TrainModule {
    public static final TrainModule NONE = new TrainModule();

    private long id;
    private DownloadState downloaded;
    private String name;
    private String url;
    private String organization;
    private String type;
    private long size;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DownloadState getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(DownloadState downloaded) {
        this.downloaded = downloaded;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
