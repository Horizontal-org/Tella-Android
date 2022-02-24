package rs.readahead.washington.mobile.views.fragment.uwazi.attachments;

import com.hzontal.tella_vault.VaultFile;

public interface ISelectorVaultHandler {
    void playMedia(VaultFile vaultFile);
    void onSelectionNumChange(int num);
    void onMediaSelected(VaultFile vaultFile);
    void onMediaDeselected(VaultFile vaultFile);
    void openFolder(VaultFile vaultFile);
}
