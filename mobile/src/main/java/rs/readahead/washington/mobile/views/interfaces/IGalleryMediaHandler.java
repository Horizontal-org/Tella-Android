package rs.readahead.washington.mobile.views.interfaces;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public interface IGalleryMediaHandler {
    void playMedia(MediaFile mediaFile);
    void onSelectionNumChange(int num);
    void onMediaSelected(MediaFile mediaFile);
    void onMediaDeselected(MediaFile mediaFile);
}
