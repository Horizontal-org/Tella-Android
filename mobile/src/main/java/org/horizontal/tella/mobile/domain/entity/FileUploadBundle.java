package org.horizontal.tella.mobile.domain.entity;

import com.hzontal.tella_vault.VaultFile;

import java.io.Serializable;

public class FileUploadBundle implements Serializable {

    private VaultFile vaultFile;
    private long serverId;
    private boolean includeMetadata;
    private boolean manualUpload;

    public FileUploadBundle(VaultFile vaultFile) {
        this.vaultFile = vaultFile;
    }

    public VaultFile getMediaFile() {
        return vaultFile;
    }

    public void setMediaFile(VaultFile vaultFile) {
        this.vaultFile = vaultFile;
    }

    public boolean isIncludeMetdata() {
        return includeMetadata;
    }

    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }

    public boolean isManualUpload() {
        return manualUpload;
    }

    public void setManualUpload(boolean manualUpload) {
        this.manualUpload = manualUpload;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }
}
