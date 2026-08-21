package org.horizontal.tella.mobile.views.interfaces;

import com.hzontal.tella_vault.VaultFile;



public interface IGalleryMediaHandler {
    void playMedia(VaultFile mediaFile);
    void onSelectionNumChange(int num);
    void onMediaSelected(VaultFile mediaFile);
    void onMediaDeselected(VaultFile mediaFile);
}
