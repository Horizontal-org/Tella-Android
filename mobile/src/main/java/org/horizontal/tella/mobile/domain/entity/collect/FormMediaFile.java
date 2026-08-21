package org.horizontal.tella.mobile.domain.entity.collect;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;


public class FormMediaFile extends VaultFile {
    public FormMediaFileStatus status; // break away from getters/setters :)
    public boolean uploading;
    public long uploadedSize;
    public String transmissionId;


     public FormMediaFile() {
        super();
        status = FormMediaFileStatus.UNKNOWN;
        uploading = true;
        uploadedSize = 0;
         transmissionId = "";
    }

    public static FormMediaFile fromMediaFile(@NonNull VaultFile vaultFile) {
        FormMediaFile formMediaFile = new FormMediaFile();
        formMediaFile.id = vaultFile.id;
        formMediaFile.created = vaultFile.created;
        formMediaFile.duration = vaultFile.duration;
        formMediaFile.metadata = vaultFile.metadata;
        formMediaFile.size = vaultFile.size;
        formMediaFile.anonymous = vaultFile.anonymous;
        formMediaFile.mimeType = vaultFile.mimeType;
        formMediaFile.thumb = vaultFile.thumb;
        formMediaFile.type = vaultFile.type;
        formMediaFile.name = vaultFile.name;
        formMediaFile.hash = vaultFile.hash;
        formMediaFile.path = vaultFile.path;
        return formMediaFile;
    }

    public VaultFile getVaultFile() {
        VaultFile vaultFile = new VaultFile();
        vaultFile.id = this.id;
        vaultFile.created = this.created;
        vaultFile.duration = this.duration;
        vaultFile.metadata = this.metadata;
        vaultFile.size = this.size;
        vaultFile.anonymous = this.anonymous;
        vaultFile.mimeType = this.mimeType;
        vaultFile.thumb = this.thumb;
        vaultFile.type = this.type;
        vaultFile.name = this.name;
        vaultFile.hash = this.hash;
        vaultFile.path = this.path;
        return vaultFile;
    }

    public String getPartName() {
        return id;
    }
}
