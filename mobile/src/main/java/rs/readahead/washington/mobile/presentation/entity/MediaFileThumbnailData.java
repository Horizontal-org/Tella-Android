package rs.readahead.washington.mobile.presentation.entity;


public class MediaFileThumbnailData {
    public static MediaFileThumbnailData NONE = new MediaFileThumbnailData(null);

    private byte[] data;


    public MediaFileThumbnailData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
