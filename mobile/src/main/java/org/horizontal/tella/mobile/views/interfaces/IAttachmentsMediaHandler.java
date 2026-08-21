package org.horizontal.tella.mobile.views.interfaces;

import com.hzontal.tella_vault.VaultFile;


public interface IAttachmentsMediaHandler {
    void playMedia(VaultFile vaultFile);
    void onRemoveAttachment(VaultFile vaultFile);
}
