package rs.readahead.washington.mobile.views.interfaces;

import com.hzontal.tella_vault.VaultFile;


public interface IReportAttachmentsHandler {
    void playMedia(VaultFile vaultFile);
    void onRemovedAttachments();
}

