package com.hzontal.tella_vault;

import java.io.InputStream;
import java.util.UUID;

public class VaultFileBuilder extends BaseVaultFileBuilder<VaultFileBuilder, VaultFile> {
    protected final Vault vault;

    private VaultFileBuilder() {
        this.vault = null;
    }

    protected VaultFileBuilder(Vault vault, String name) {
        this(vault, name, null);
    }

    protected VaultFileBuilder(Vault vault, InputStream data) {
        this(vault, null, data);

        this.name = this.id;
    }

    protected VaultFileBuilder(Vault vault, String name, InputStream data) {
        this.vault = vault;
        this.id = UUID.randomUUID().toString();
        this.type = VaultFile.Type.FILE; // default
        this.name = name;
        this.data = data;
    }

    public VaultFile build() throws VaultException {
        return vault.create(this);
    }

    public VaultFile build(String parentId) throws VaultException {
        return vault.create(this, parentId);
    }

    @Override
    protected VaultFileBuilder getThis() {
        return this;
    }
}
