package com.hzontal.tella_vault.rx;

import android.content.Context;

import com.hzontal.tella_vault.BaseVault;
import com.hzontal.tella_vault.BaseVaultFileBuilder;
import com.hzontal.tella_vault.IVaultDatabase;
import com.hzontal.tella_vault.Vault;
import com.hzontal.tella_vault.VaultException;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.database.VaultDataSource;

import org.hzontal.tella.keys.key.LifecycleMainKey;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;


public class RxVault extends BaseVault {
    public RxVault(Context context, Vault vault) throws LifecycleMainKey.MainKeyUnavailableException, VaultException {
        this(context, vault.getMainKeyHolder(), vault.getConfig());
    }

    public RxVault(Context context, LifecycleMainKey mainKeyHolder, Config config)
            throws VaultException, LifecycleMainKey.MainKeyUnavailableException {
        this(mainKeyHolder, config, VaultDataSource.getInstance(context, mainKeyHolder.get().getKey().getEncoded()));
    }

    public RxVault(LifecycleMainKey mainKeyHolder, Config config, IVaultDatabase database) throws VaultException {
        super(mainKeyHolder, config, database);
    }

    public RxVaultFileBuilder builder(String name, InputStream data) {
        return new RxVaultFileBuilder(this, name, data);
    }

    public RxVaultFileBuilder builder(String name) {
        return new RxVaultFileBuilder(this, name);
    }

    public RxVaultFileBuilder builder(InputStream data) {
        return new RxVaultFileBuilder(this, data);
    }

    public InputStream getStream(VaultFile vaultFile) throws VaultException {
        return baseGetStream(vaultFile);
    }

    public OutputStream getOutStream(VaultFile vaultFile) throws VaultException {
        return baseOutStream(vaultFile);
    }

    public Single<VaultFile> getRoot() {
        return Single.defer(() -> {
            try {
                return Single.just(baseGetRoot());
            } catch (Exception e) {
                return Single.error(e);
            }
        });
    }

    public Single<Boolean> delete(VaultFile file) {
        return Single.defer(() -> {
            try {
                return Single.just(baseDelete(file));
            } catch (Exception e) {
                return Single.error(e);
            }
        });
    }

    public Single<List<VaultFile>> list(VaultFile parent) {
        return Single.defer(() -> {
            try {
                return Single.just(baseList(parent));
            } catch (Exception e) {
                return Single.error(e);
            }
        });
    }

    public Single<List<VaultFile>> list(VaultFile parent, IVaultDatabase.Filter filter, IVaultDatabase.Sort sort, IVaultDatabase.Limits limits) {
        return Single.defer(() -> {
            try {
                return Single.just(baseList(parent, filter, sort, limits));
            } catch (Exception e) {
                return Single.error(e);
            }
        });
    }

    public Single<VaultFile> get(String id) {
        return Single.defer(() -> {
            try {
                return Single.just(baseGet(id));
            } catch (Exception e) {
                return Single.error(e);
            }
        });
    }

    public Single<VaultFile> update(VaultFile vaultFile) {
        return Single.defer(() -> {
            try {
                return Single.just(baseUpdate(vaultFile));
            } catch (Exception e) {
                return Single.error(e);
            }
        });
    }

    public Completable destroy() {
        return Completable.defer(() -> {
            try {
                baseDestroy();
                return Completable.complete();
            } catch (Exception e) {
                return Completable.error(e);
            }
        });
    }

    protected Single<VaultFile> create(BaseVaultFileBuilder<?, ?> builder) {
        return Single.defer(() -> {
            try {
                return Single.just(baseCreate(builder));
            } catch (Exception e) {
                return Single.error(e);
            }
        });
    }
}
