package org.horizontal.tella.mobile.domain.entity;

import java.io.Serializable;


public abstract class Server implements Serializable {
    private long id;
    private String name;
    private String url;
    private String username;
    private String password;
    private boolean checked;
    private ServerType serverType;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public ServerType getServerType() {
        return serverType;
    }

    protected void setServerType(ServerType serverType) {
        this.serverType = serverType;
    }
}
