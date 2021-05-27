package com.hzontal.tella_vault;

import android.content.Context;

import com.hzontal.tella_vault.database.VaultDataSource;

import org.hzontal.tella.keys.key.LifecycleMainKey;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


public class Vault extends BaseVault {
    public Vault(Context context, LifecycleMainKey mainKeyHolder, Config config)
            throws VaultException, LifecycleMainKey.MainKeyUnavailableException {
        this(mainKeyHolder, config, VaultDataSource.getInstance(context, mainKeyHolder.get().getKey().getEncoded()));
    }

    public Vault(LifecycleMainKey mainKeyHolder, Config config, IVaultDatabase database) throws VaultException {
        super(mainKeyHolder, config, database);
    }

    /**
     * Creates and returns new VaultFileBuilder that can be used to add new data to Vault.
     * @param name VaultFile name.
     * @param data Data to be save for VaultFile.
     * @return Creator that can be used to define file or directory.
     */
    public VaultFileBuilder builder(String name, InputStream data) {
        return new VaultFileBuilder(this, name, data);
    }

    /**
     * Creates and returns new VaultFileBuilder that can be used to add new data to Vault.
     * @param name Directory name.
     * @return Creator that can be used to define file or directory.
     */
    public VaultFileBuilder builder(String name) {
        return new VaultFileBuilder(this, name);
    }

    /**
     * Creates and returns new VaultFileBuilder that can be used to add new data to Vault.
     * @param data Data to be save for VaultFile.
     * @return Creator that can be used to define file.
     */
    public VaultFileBuilder builder(InputStream data) {
        return new VaultFileBuilder(this, data);
    }

    /**
     * Returns a stream of VaultFile's data. For directories empty InputStream will be returned.
     * @param vaultFile Data to read.
     * @return Stream of data.
     */
    public InputStream getStream(VaultFile vaultFile) throws VaultException {
        return baseGetStream(vaultFile);
    }

    /**
     * Returns a stream of VaultFile's data. For directories empty InputStream will be returned.
     * @param vaultFile Data to read.
     * @return Stream of data.
     */
    public OutputStream getOutStream(VaultFile vaultFile) throws VaultException {
        return baseOutStream(vaultFile);
    }

    /**
     * Get root VaultFile of this Vault.
     * @return The root.
     */
    public VaultFile getRoot() {
        return baseGetRoot();
    }

    /**
     * Deletes a VaultFile.
     * @param file VaultFile to delete.
     */
    public boolean delete(VaultFile file) {
        return baseDelete(file);
    }

    /**
     * List all files in path.
     * @param parent Parent VaultFile or null for root listing.
     * @return List of vault files.
     */
    public List<VaultFile> list(VaultFile parent) {
        return baseList(parent);
    }

    /**
     * Destroys whole Vault deleting data on disk and in database.
     */
    public void destroy() {
        baseDestroy();
    }

    protected VaultFile create(BaseVaultFileBuilder<?, ?> builder) throws VaultException {
        return baseCreate(builder);
    }
}
