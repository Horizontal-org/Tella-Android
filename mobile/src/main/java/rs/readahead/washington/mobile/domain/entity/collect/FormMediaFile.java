package rs.readahead.washington.mobile.domain.entity.collect;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;


//TO keep it or remove it ?
public class FormMediaFile extends VaultFile {
    public FormMediaFileStatus status; // break away from getters/setters :)
    public boolean uploading;

    private FormMediaFile() {
        super();
        status = FormMediaFileStatus.UNKNOWN;
        uploading = true;
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

    public String getPartName() {
        return name;
    }
}
