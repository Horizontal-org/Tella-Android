package rs.readahead.washington.mobile.presentation.entity;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class MediaFilesData implements Serializable {
    private List<MediaFile> mediaFiles;


    public MediaFilesData() {
        mediaFiles = new ArrayList<>();
    }

    public MediaFilesData(@NonNull List<MediaFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    @NonNull
    public List<MediaFile> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(@NonNull List<MediaFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }
}
