package org.horizontal.tella.mobile.views.interfaces;

import com.hzontal.tella_vault.VaultFile;


public interface IGalleryVaultHandler {
    void playMedia(VaultFile vaultFile);
    void onSelectionNumChange(int num);
    void onMediaSelected(VaultFile vaultFile);
    void onMediaDeselected(VaultFile vaultFile);
}
