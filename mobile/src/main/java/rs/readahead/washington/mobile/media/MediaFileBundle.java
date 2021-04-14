package rs.readahead.washington.mobile.media;

import com.hzontal.tella_vault.VaultFile;

import rs.readahead.washington.mobile.presentation.entity.MediaFileThumbnailData;


public class MediaFileBundle {
    private VaultFile vaultFile;
    private MediaFileThumbnailData mediaFileThumbnailData = MediaFileThumbnailData.NONE;


    public VaultFile getVaultFile() {
        return vaultFile;
    }

    public void setMediaFile(VaultFile vaultFile) {
        this.vaultFile = vaultFile;
    }

    public MediaFileThumbnailData getMediaFileThumbnailData() {
        return mediaFileThumbnailData;
    }

    public void setMediaFileThumbnailData(MediaFileThumbnailData mediaFileThumbnailData) {
        this.mediaFileThumbnailData = mediaFileThumbnailData;
    }
}
