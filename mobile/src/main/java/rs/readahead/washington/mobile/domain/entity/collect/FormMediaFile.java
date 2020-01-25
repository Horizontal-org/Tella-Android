package rs.readahead.washington.mobile.domain.entity.collect;

import androidx.annotation.NonNull;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class FormMediaFile extends MediaFile {
    public FormMediaFileStatus status; // break away from getters/setters :)
    public boolean uploading;

    private FormMediaFile(String path, String uid, String filename, Type type) {
        super(path, uid, filename, type);

        status = FormMediaFileStatus.UNKNOWN;
        uploading = true;
    }

    public static FormMediaFile fromMediaFile(@NonNull MediaFile mediaFile) {
        FormMediaFile formMediaFile = new FormMediaFile(
                mediaFile.getPath(),
                mediaFile.getUid(),
                mediaFile.getFileName(),
                mediaFile.getType()
        );
        formMediaFile.setId(mediaFile.getId());
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
