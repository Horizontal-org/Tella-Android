package rs.readahead.washington.mobile.domain.entity.collect;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;

//TO keep it or remove it ?
public class FormMediaFile extends VaultFile {
    public FormMediaFileStatus status; // break away from getters/setters :)
    public boolean uploading;

    private FormMediaFile() {
    }

    public static FormMediaFile fromMediaFile(@NonNull VaultFile vaultFile) {
        FormMediaFile formMediaFile = new FormMediaFile();

        formMediaFile.id = vaultFile.id;
        formMediaFile.name = vaultFile.name;
        formMediaFile.created = vaultFile.created;
        formMediaFile.duration = vaultFile.duration;
        formMediaFile.metadata = vaultFile.metadata;
        formMediaFile.size = vaultFile.size;
        formMediaFile.mimeType = vaultFile.mimeType;
        formMediaFile.anonymous = vaultFile.anonymous;
        formMediaFile.hash = vaultFile.hash;
        formMediaFile.thumb = vaultFile.thumb;
        formMediaFile.status = FormMediaFileStatus.UNKNOWN;
        formMediaFile.uploading = true;

        return formMediaFile;
    }

    public String getPartName() {
        return name;
    }
}
