package rs.readahead.washington.mobile.data;

import com.hzontal.tella_vault.VaultFile;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.database.KeyDataSource;
import rs.readahead.washington.mobile.data.entity.mapper.EntityMapper;
import timber.log.Timber;

public class Tella2UpgradeHandler {
    /**
     * Copy DB data from MediaFiles to VaultFiles and deletes rows in MediaFile table.
     * @param keyDataSource KeyDataSource
     * @param keyRxVault KeyRxVault
     * @return When completes.
     */
    public static Completable copyMediaFilesToVault(KeyDataSource keyDataSource, KeyRxVault keyRxVault) {
        return keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapSingle(DataSource::listAllMediaFiles)
                .flatMapIterable(mediaFileUpgradeData -> mediaFileUpgradeData)
                .flatMap(data -> createVaultFile(keyRxVault, data))
                .toList()
                .flatMapCompletable(vaultFiles -> deleteMediaFiles(keyDataSource));
    }

    private static Observable<VaultFile> createVaultFile(KeyRxVault keyRxVault, DataSource.MediaFileUpgradeData mediaFileUpgradeData) {
        Timber.d("***** Tella2UpgradeHandler.createVaultFile");

        return keyRxVault.getRxVault()
                .flatMap(vault ->
                        vault.builder()
                                .setId(mediaFileUpgradeData.id)
                                .setName(mediaFileUpgradeData.name)
                                .setType(VaultFile.Type.FILE)
                                .setMimeType(mediaFileUpgradeData.mimeType)
                                .setAnonymous(mediaFileUpgradeData.anonymous)
                                .setThumb(mediaFileUpgradeData.thumb)
                                .setDuration(mediaFileUpgradeData.duration)
                                .setHash(mediaFileUpgradeData.hash)
                                .setSize(mediaFileUpgradeData.size)
                                .setMetadata(new EntityMapper().transform(mediaFileUpgradeData.metadata))
                                .setPath(mediaFileUpgradeData.path) // todo: check this
                                .build()
                                .toObservable()
                );
    }

    private static Completable deleteMediaFiles(KeyDataSource keyDataSource) {
        Timber.d("***** Tella2UpgradeHandler.deleteMediaFiles");

        return keyDataSource.getDataSource()
                .subscribeOn(Schedulers.io())
                .flatMapCompletable(DataSource::deleteAllMediaFiles);
    }
}
