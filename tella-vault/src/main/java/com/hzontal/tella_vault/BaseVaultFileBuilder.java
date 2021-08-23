package com.hzontal.tella_vault;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

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
        checkForFile();
        this.mimeType = mimeType;
        return getThis();
    }

    public T setMetadata(Metadata metadata) {
        checkForFile();
        this.metadata = metadata;
        return getThis();
    }

    public T setThumb(byte[] thumb) {
        checkForFile();
        this.thumb = thumb;
        return getThis();
    }

    public T setDuration(long duration) {
        checkForFile();
        this.duration = duration;
        return getThis();
    }

    public T setAnonymous(boolean anonymous) {
        checkForFile();
        this.anonymous = anonymous;
        return getThis();
    }

    public T setId(String id) {
        this.id = id;
        return getThis();
    }

    public T setData(InputStream data) {
        checkForFile();
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
        checkForFile();
        this.hash = hash;
        return getThis();
    }

    abstract public B build() throws Exception;

    /* package */
    static String getExtensionFromMimeType(String mimeType) {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean validateVaultFile() {
        if (this.type == null) {
            return false;
        }

        setDefaultMimeType();
        setDefaultName();

        return true;
    }

    protected void setDefaultMimeType() {
        if (this.type == VaultFile.Type.FILE && TextUtils.isEmpty(this.mimeType)) {
            setMimeType("application/octet-stream");
        }
    }

    protected void setDefaultName() {
        if (TextUtils.isEmpty(this.name)) {
            this.name = this.id;

            if (this.type == VaultFile.Type.FILE) {
                this.name = this.name + "." + getExtensionFromMimeType(this.mimeType);
            }
        }
    }

    protected void checkForFile() {
        if (this.type != VaultFile.Type.FILE) {
            throw new IllegalStateException("Required FILE type for this operation");
        }
    }

    abstract protected T getThis();
}
