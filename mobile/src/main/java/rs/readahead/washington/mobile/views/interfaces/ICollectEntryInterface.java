package rs.readahead.washington.mobile.views.interfaces;

import com.hzontal.tella_vault.VaultFile;

public interface ICollectEntryInterface {
    void openAudioRecorder();

    void returnFileToForm(VaultFile file);

    void stopWaitingForData();
}
