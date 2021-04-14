package rs.readahead.washington.mobile.domain.entity.collect;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;



public class FormMediaFile extends VaultFile {
    public FormMediaFileStatus status; // break away from getters/setters :)
    public boolean uploading;

    private FormMediaFile(String path, String uid, String filename, Type type) {
        //super(path, uid, filename, type);

        status = FormMediaFileStatus.UNKNOWN;
        uploading = true;
    }

    public static FormMediaFile fromMediaFile(@NonNull VaultFile vaultFile) {
        FormMediaFile formMediaFile = new FormMediaFile(
                vaultFile.path,
                vaultFile.id,
                vaultFile.name,
                vaultFile.type
        );
        formMediaFile.id = mediaFile.getId();
        formMediaFile.setCreated(mediaFile.getCreated());
        formMediaFile.setDuration(mediaFile.getDuration());
        formMediaFile.setMetadata(mediaFile.getMetadata());
        formMediaFile.setSize(mediaFile.getSize());
        formMediaFile.setAnonymous(mediaFile.isAnonymous());

        return formMediaFile;
    }

    public String getPartName() {
        return fileName;
    }
}
