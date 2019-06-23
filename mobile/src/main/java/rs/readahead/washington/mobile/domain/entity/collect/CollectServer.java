package rs.readahead.washington.mobile.domain.entity.collect;

import java.io.Serializable;


public class CollectServer implements Serializable {
    public static final CollectServer NONE = new CollectServer();

    private long id;
    private String name;
    private String url;
    private String username;
    private String password;
    private boolean checked;


    public CollectServer() {
    }

    public CollectServer(long id) {
        this.id = id;
    }

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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (! (obj instanceof CollectServer)) {
            return false;
        }

        final CollectServer that = (CollectServer) obj;

        return this.id == that.id;
    }
}
