package com.hzontal.tella_vault.filter;

import com.hzontal.tella_vault.VaultFile;

public interface Filter {
        boolean applyFilter(VaultFile vaultFile);
    }