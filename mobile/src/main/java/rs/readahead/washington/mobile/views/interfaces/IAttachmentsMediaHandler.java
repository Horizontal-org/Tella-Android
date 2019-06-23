package rs.readahead.washington.mobile.views.interfaces;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public interface IAttachmentsMediaHandler {
    void playMedia(MediaFile mediaFile);
    void onRemoveAttachment(MediaFile mediaFile);
}
