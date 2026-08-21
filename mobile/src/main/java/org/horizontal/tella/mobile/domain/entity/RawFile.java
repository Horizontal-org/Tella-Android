package org.horizontal.tella.mobile.domain.entity;

import android.text.TextUtils;

import java.io.Serializable;

import androidx.annotation.NonNull;


public class RawFile implements Serializable, Comparable {
    private long id;
    String uid;
    protected String path;
    protected String fileName;
    private long size; // bytes
    private long created;
    private boolean anonymous;

    RawFile() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof RawFile)) {
            return false;
        }

        final RawFile that = (RawFile) obj;

        return TextUtils.equals(this.uid, that.uid);
    }

    @Override
    public int hashCode() {
        if (uid != null) {
            return uid.hashCode();
        }

        return super.hashCode();
    }

    @Override
    public int compareTo(@NonNull Object o) {
        if (!(o instanceof RawFile)) {
            return 0;
        } else {
            return (int) (this.created - ((RawFile) o).created);
        }
    }
}
