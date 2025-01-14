package org.horizontal.tella.mobile.domain.entity;

import com.hzontal.tella_vault.VaultFile;

import java.io.Serializable;

import org.horizontal.tella.mobile.domain.repository.ITellaUploadsRepository.UploadStatus;

public class FileUploadInstance implements Serializable {
    private long id;
    private VaultFile vaultFile;
    private long updated;
    private long started;
    private UploadStatus status;
    private long size;
    private long uploaded;
    //TODO MAYBE ESCAPE IT FOR NOW
    private int retryCount;
    private long set;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public VaultFile getMediaFile() {
        return vaultFile;
    }

    public void setMediaFile(VaultFile vaultFile) {
        this.vaultFile = vaultFile;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public long getStarted() {
        return started;
    }

    public void setStarted(long started) {
        this.started = started;
    }

    public UploadStatus getStatus() {
        return status;
    }

    public void setStatus(UploadStatus status) {
        this.status = status;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getUploaded() {
        return uploaded;
    }

    public void setUploaded(long uploaded) {
        this.uploaded = uploaded;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getSet() {
        return set;
    }

    public void setSet(long set) {
        this.set = set;
    }
}
