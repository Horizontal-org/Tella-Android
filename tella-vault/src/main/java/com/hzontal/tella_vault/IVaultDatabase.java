package com.hzontal.tella_vault;

import java.util.List;

public interface IVaultDatabase {
    VaultFile getRootVaultFile();
    VaultFile create(VaultFile file);
    List<VaultFile> list(VaultFile parent, Filter filter, Sort sort, Limits limits);
    VaultFile get(String id);
    List<VaultFile> get(String[] ids);
    boolean delete(VaultFile file, IVaultFileDeleter deleter);
    void destroy();

    interface IVaultFileDeleter {
        boolean delete(VaultFile vaultFile);
    }

    interface Filter {
    }

    interface Sort {
    }

    class Limits {
        public int offset;
        public int limit;
    }
}
