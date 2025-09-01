package com.hzontal.tella_vault.exceptions;

import com.hzontal.tella_vault.VaultException;
import com.hzontal.tella_vault.VaultFile;

/**
 * Thrown when a file with the same hash already exists in the vault.
 */
public class DuplicateVaultFileException extends VaultException {
    private final VaultFile existingFile;

    public DuplicateVaultFileException(VaultFile existingFile) {
        super("Duplicate VaultFile with hash: " + existingFile.hash);
        this.existingFile = existingFile;
    }

    public VaultFile getExistingFile() {
        return existingFile;
    }
}
