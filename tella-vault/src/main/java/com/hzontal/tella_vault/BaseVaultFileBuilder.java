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

    // todo: with all other options.. like withThumbnail()

    abstract public B build() throws Exception;

    abstract protected T getThis();
}
