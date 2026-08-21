package org.horizontal.tella.mobile.bus.event;

import com.hzontal.tella_vault.VaultFile;

public class AudioRecordEvent {

    private VaultFile vaultFile;

    public AudioRecordEvent(VaultFile vaultFile) {
        this.vaultFile = vaultFile;
    }

    public VaultFile getVaultFile() {
        return vaultFile;
    }
}

