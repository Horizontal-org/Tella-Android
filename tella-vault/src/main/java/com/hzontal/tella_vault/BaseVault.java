package com.hzontal.tella_vault;

import android.content.Context;
import android.text.TextUtils;

import com.hzontal.tella_vault.database.VaultDataSource;
import com.hzontal.tella_vault.filter.FilterType;
import com.hzontal.tella_vault.filter.Limits;
import com.hzontal.tella_vault.filter.Sort;
import com.hzontal.utils.FileUtil;

import org.apache.commons.io.IOUtils;
import org.hzontal.tella.keys.key.LifecycleMainKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import androidx.annotation.WorkerThread;


/**
 * Vault stores supplied data as encrypted files with supplied metadata, thumbnails and actual
 * data that is provided as InputStream. Supports simple filesystem structure with files and
 * directories.
 */
public abstract class BaseVault {
    public static LifecycleMainKey mainKeyHolder = null; // todo: this should be interface with `MainKey get()` method
    protected final IVaultDatabase database;
    protected final Config config;
    protected final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public BaseVault(Context context, LifecycleMainKey mainKeyHolder, Config config)
            throws VaultException, LifecycleMainKey.MainKeyUnavailableException {
        this(mainKeyHolder, config, VaultDataSource.getInstance(context, mainKeyHolder.get().getKey().getEncoded()));
    }

    public BaseVault(LifecycleMainKey mainKeyHolder, Config config, IVaultDatabase database) throws VaultException {
        BaseVault.mainKeyHolder = mainKeyHolder;
        this.database = database;
        this.config = config;

        if (!mkdirs(config.root)) {
            throw new VaultException("Unable to create root directory");
        }
    }

    public Config getConfig() {
        return config;
    }

    public LifecycleMainKey getMainKeyHolder() {
        return mainKeyHolder;
    }

    /**
     * Returns a stream of VaultFile's data. For directories empty InputStream will be returned.
     * @param vaultFile Data to read.
     * @return Stream of data.
     */
    protected InputStream baseGetStream(VaultFile vaultFile) throws VaultException {
        try {
            File file = getFile(vaultFile);
            FileInputStream fis = new FileInputStream(file);
            byte[] key = mainKeyHolder.get().getKey().getEncoded();

            return CipherStreamUtils.getDecryptedLimitedInputStream(key, fis, file);

        } catch (IOException | LifecycleMainKey.MainKeyUnavailableException e) {
            throw new VaultException(e);
        }
    }

    /**
     * Returns a stream of VaultFile's data. For directories empty OutputStream will be returned.
     * @param vaultFile Data to read.
     * @return Stream of data.
     */
    protected VaultOutputStream baseOutStream(VaultFile vaultFile) throws VaultException {
        try {
            File file = getWritableFile(vaultFile);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] key = mainKeyHolder.get().getKey().getEncoded();

            return new VaultOutputStream(vaultFile, CipherStreamUtils.getEncryptedOutputStream(key, fos, file.getName()),
                    MessageDigest.getInstance("SHA-256"));
        } catch (IOException | LifecycleMainKey.MainKeyUnavailableException | NoSuchAlgorithmException e) {
            throw new VaultException(e);
        }
    }

    /**
     * Get root VaultFile of this Vault.
     * @return The root.
     */
    protected VaultFile baseGetRoot() {
        return database.getRootVaultFile();
    }

    /**
     * Deletes a VaultFile.
     * @param file VaultFile to delete.
     */
    protected boolean baseDelete(VaultFile file) {
        return database.delete(file, deleted ->
                deleted.type == VaultFile.Type.DIRECTORY || getFile(deleted).delete());
    }

    /**
     * Deletes a VaultFile.
     * @param id VaultFile id to rename.
     * @param name New VaultFile name
     */
    protected VaultFile baseRename(String id, String name) {
        return database.rename(id,name);
    }


    /**
     * List all files in path.
     * @param parent Parent VaultFile or null for root listing.
     * @return List of vault files.
     */
    protected List<VaultFile> baseList(VaultFile parent) {
        return database.list(parent, null, null, null); // todo: design filter and sort that will handle use-cases
    }


    /**
     * List all files in path.
     * @param parent Parent VaultFile or null for root listing.
     * @return List of vault files.
     */
    protected List<VaultFile> baseList(VaultFile parent, FilterType filterType, Sort sort, Limits limits) {
        return database.list(parent, filterType, sort, limits); // todo: design filter and sort that will handle use-cases
    }

    /**
     * Destroys whole Vault deleting data on disk and in database.
     */
    protected void baseDestroy() {
        database.destroy();
    }

    protected VaultFile baseGet(String id){
        return database.get(id);
    }

    protected Set<VaultFile> baseGet(Set<String> ids) {
        return database.get(ids);
    }

    protected VaultFile baseCreate(BaseVaultFileBuilder<?, ?> builder) throws VaultException {
        return baseCreate(builder, VaultDataSource.ROOT_UID);
    }

    protected VaultFile baseCreate(BaseVaultFileBuilder<?, ?> builder, String parentId) throws VaultException {
        try {
            VaultFile vaultFile = new VaultFile(builder);

            if (builder.data != null) { // not a dir
                File file = getWritableFile(vaultFile);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] key = mainKeyHolder.get().getKey().getEncoded();

                DigestOutputStream os = new DigestOutputStream(
                        CipherStreamUtils.getEncryptedOutputStream(key, fos, file.getName()), // todo: change this
                        MessageDigest.getInstance("SHA-256"));

                IOUtils.copy(builder.data, os);
                FileUtil.close(builder.data);
                FileUtil.close(os);

                vaultFile.hash = hexString(os.getMessageDigest().digest());
                vaultFile.size = getSize(file);
            }

            return database.create(parentId, vaultFile);
        } catch (IOException | NoSuchAlgorithmException | LifecycleMainKey.MainKeyUnavailableException e) {
            throw new VaultException(e);
        }
    }

    protected VaultFile baseUpdateMetadata(VaultFile vaultFile, Metadata metadata) {
        return database.updateMetadata(vaultFile, metadata);
    }

    public static class Config { // todo: make this VaultConfig
        /**
         * Filesystem root where Vault should create content files if chosen
         * to be saved on file system.
         */
        public File root;
    }

    protected String hexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    protected long getSize(File file) {
        return file.length() - CipherStreamUtils.IV_SIZE;
    }

    /**
     * Returns File that holds encrypted contents of VaultFile.
     * @param vaultFile VaultFile that we need File with contents.
     * @return File holding contents of VaultFile.
     */
    public File getFile(VaultFile vaultFile) {
        File file = new File(this.config.root, getFileName(vaultFile));
        //noinspection ResultOfMethodCallIgnored
        file.setReadOnly();
        return file;
    }

    protected File getWritableFile(VaultFile vaultFile) {
        return new File(this.config.root, getFileName(vaultFile));
    }

    protected boolean mkdirs(File path) {
        return path.exists() || path.mkdirs();
    }

    protected String getFileName(VaultFile vaultFile) {
        String ext = VaultFileBuilder.getExtensionFromMimeType(vaultFile.mimeType);
        return vaultFile.id + (!TextUtils.isEmpty(ext) ? "." + ext : "");
    }

    public class VaultOutputStream extends DigestOutputStream {
        private final VaultFile vaultFile;

        public VaultOutputStream(VaultFile vaultFile, OutputStream stream, MessageDigest digest) {
            super(stream, digest);

            this.vaultFile = vaultFile;
        }

        @WorkerThread
        public VaultFile complete(long duration) {
            vaultFile.hash = hexString(getMessageDigest().digest());
            vaultFile.size = getSize(getFile(vaultFile));
            vaultFile.duration = duration;

            return database.completeVaultOutputStream(vaultFile);
        }
    }
}
