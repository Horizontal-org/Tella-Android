package com.hzontal.tella_vault;

import java.io.InputStream;

public abstract class BaseVaultFileBuilder<T extends BaseVaultFileBuilder<T, B>, B> {
    protected String id;
    protected VaultFile parent;
    protected VaultFile.Type type;
    protected String mimeType;
    protected String name;
    protected long size;
    protected long duration;
    protected boolean anonymous;
    protected Metadata metadata;
    protected byte[] thumb;
    protected InputStream data;
    protected String hash;
    protected String path;

    public T setPath(String path) {
        this.path = path;
        return getThis();
    }

    public T setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return getThis();
    }

    public T setMetadata(Metadata metadata) {
        this.metadata = metadata;
        return getThis();
    }

    public T setThumb(byte[] thumb) {
        this.thumb = thumb;
        return getThis();
    }

    public T setDuration(long duration) {
        this.duration = duration;
        return getThis();
    }

    public T setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
        return getThis();
    }

    public T  setId(String id) {
        this.id = id;
        return getThis();
    }

    public T setData(InputStream data) {
        this.data = data;
        return getThis();
    }

    public T setName(String name) {
        this.name = name;
        return getThis();
    }

    public T setParent(VaultFile parent) {
        this.parent = parent;
        return getThis();
    }

    public T setSize(long size) {
        this.size = size;
        return getThis();
    }

    public T setType(VaultFile.Type type) {
        this.type = type;
        return getThis();
    }

    public T setHash(String hash) {
        this.hash = hash;
        return getThis();
    }

    abstract public B build() throws Exception;

    abstract protected T getThis();
}
