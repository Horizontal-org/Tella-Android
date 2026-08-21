package org.horizontal.tella.mobile.presentation.entity;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.VaultFile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;



public class MediaFilesData implements Serializable {
    private List<VaultFile> mediaFiles;


    public MediaFilesData() {
        mediaFiles = new ArrayList<>();
    }

    public MediaFilesData(@NonNull List<VaultFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    @NonNull
    public List<VaultFile> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(@NonNull List<VaultFile> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }
}
