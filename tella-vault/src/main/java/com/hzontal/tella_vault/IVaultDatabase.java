package com.hzontal.tella_vault;

import java.util.List;

public interface IVaultDatabase {
    VaultFile getRootVaultFile();
    VaultFile create(String parentId, VaultFile file);
    List<VaultFile> list(VaultFile parent, Filter filter, Sort sort, Limits limits);
    VaultFile updateMetadata(VaultFile vaultFile, Metadata metadata);
    VaultFile completeVaultOutputStream(VaultFile vaultFile);
    VaultFile get(String id);
    VaultFile rename(String id,String name);
    List<VaultFile> get(String[] ids);
    boolean delete(VaultFile file, IVaultFileDeleter deleter);
    void destroy();

    interface IVaultFileDeleter {
        boolean delete(VaultFile vaultFile);
    }


    class Limits {
        public int offset;
        public int limit;
    }

    class Sort {
        public enum Direction {
            ASC,
            DESC;
        }

        public Direction direction;
        public String property;
    }
}
