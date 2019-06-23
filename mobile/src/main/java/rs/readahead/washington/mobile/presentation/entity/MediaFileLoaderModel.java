package rs.readahead.washington.mobile.presentation.entity;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class MediaFileLoaderModel {
    private MediaFile mediaFile;
    private LoadType loadType;


    public MediaFileLoaderModel(MediaFile mediaFile, LoadType loadType) {
        this.mediaFile = mediaFile;
        this.loadType = loadType;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public LoadType getLoadType() {
        return loadType;
    }

    public enum LoadType {
        ORIGINAL,
        THUMBNAIL
    }
}
