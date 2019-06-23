package rs.readahead.washington.mobile.media;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class MediaFileBundle {
    private MediaFile mediaFile;
    private MediaFileThumbnailData mediaFileThumbnailData = MediaFileThumbnailData.NONE;


    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(MediaFile mediaFile) {
        this.mediaFile = mediaFile;
    }

    public MediaFileThumbnailData getMediaFileThumbnailData() {
        return mediaFileThumbnailData;
    }

    public void setMediaFileThumbnailData(MediaFileThumbnailData mediaFileThumbnailData) {
        this.mediaFileThumbnailData = mediaFileThumbnailData;
    }
}
