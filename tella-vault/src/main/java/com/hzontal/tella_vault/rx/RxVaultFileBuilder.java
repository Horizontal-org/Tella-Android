package com.hzontal.tella_vault.rx;

import com.hzontal.tella_vault.BaseVaultFileBuilder;
import com.hzontal.tella_vault.VaultFile;

import java.io.InputStream;
import java.util.UUID;

import io.reactivex.Single;


public class RxVaultFileBuilder extends BaseVaultFileBuilder<RxVaultFileBuilder, Single<VaultFile>> {
    protected RxVault vault;

    private RxVaultFileBuilder() {
        this.vault = null;
    }

    protected RxVaultFileBuilder(RxVault vault, String name) {
        this(vault, name, null);
    }

    protected RxVaultFileBuilder(RxVault vault, InputStream data) {
        this(vault, null, data);

        this.name = this.id;
    }

    protected RxVaultFileBuilder(RxVault vault, String name, InputStream data) {
        this.vault = vault;
        this.id = UUID.randomUUID().toString();
        this.type = VaultFile.Type.FILE; // default
        this.name = name;
        this.data = data;
    }

    public Single<VaultFile> build() {
        return vault.create(this);
    }

    @Override
    protected RxVaultFileBuilder getThis() {
        return this;
    }
}
