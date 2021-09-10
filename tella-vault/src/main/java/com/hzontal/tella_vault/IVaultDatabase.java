package com.hzontal.tella_vault;


import com.hzontal.tella_vault.filter.Filter;
import com.hzontal.tella_vault.filter.FilterType;
import com.hzontal.tella_vault.filter.Limits;
import com.hzontal.tella_vault.filter.Sort;

import java.util.List;

public interface IVaultDatabase {
    VaultFile getRootVaultFile();
    VaultFile create(String parentId, VaultFile file);
    List<VaultFile> list(VaultFile parent, FilterType filter, Sort sort, Limits limits);
    VaultFile updateMetadata(VaultFile vaultFile, Metadata metadata);
    VaultFile completeVaultOutputStream(VaultFile vaultFile);
    VaultFile get(String id);
    VaultFile rename(String id,String name);
    boolean move(VaultFile vaultFile, VaultFile newParent);
    List<VaultFile> get(String[] ids);
    boolean delete(VaultFile file, IVaultFileDeleter deleter);
    void destroy();

    interface IVaultFileDeleter {
        boolean delete(VaultFile vaultFile);
    }
}
