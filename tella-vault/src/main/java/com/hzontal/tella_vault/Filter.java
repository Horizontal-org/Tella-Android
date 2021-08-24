package com.hzontal.tella_vault;

public interface Filter {
        enum FilterType {
            ALL,
            PHOTO,
            VIDEO,
            AUDIO,
            WITH_METADATA,
            WITHOUT_METADATA
        }

        boolean applyFilter(VaultFile vaultFile);
    }