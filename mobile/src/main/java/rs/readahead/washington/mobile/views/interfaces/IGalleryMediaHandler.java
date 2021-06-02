package rs.readahead.washington.mobile.views.interfaces;

import com.hzontal.tella_vault.VaultFile;


public interface IGalleryMediaHandler {
    void playMedia(VaultFile vaultFile);
    void onSelectionNumChange(int num);
    void onMediaSelected(VaultFile vaultFile);
    void onMediaDeselected(VaultFile vaultFile);
}
