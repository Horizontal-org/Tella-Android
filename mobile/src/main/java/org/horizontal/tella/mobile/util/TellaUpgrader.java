package org.horizontal.tella.mobile.util;

import static com.hzontal.tella_vault.database.VaultDataSource.ROOT_UID;

import android.content.Context;

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.database.VaultDataSource;
import com.hzontal.utils.MediaFile;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.horizontal.tella.mobile.data.database.DataSource;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.domain.entity.OldMediaFile;
import org.horizontal.tella.mobile.domain.entity.collect.CollectInstanceVaultFile;


public class TellaUpgrader {

    public static void upgradeV2(Context context, byte[] key) {
        DataSource dataSource = DataSource.getInstance(context, key);
        VaultDataSource vaultDataSource = VaultDataSource.getInstance(context, key);
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            {
                List<OldMediaFile> allMediaFiles = dataSource.listOldMediaFiles().blockingGet();
                for (OldMediaFile mediaFile : allMediaFiles) {
                    vaultDataSource.create(ROOT_UID, getVaultFile(mediaFile));
                }
                List<CollectInstanceVaultFile> collectInstanceVaultFiles = dataSource.getCollectInstanceVaultFilesDB();

                if (dataSource.insertCollectInstanceVaultFiles(collectInstanceVaultFiles)) {
                    Preferences.setUpgradeTella2(false);
                }
            }
        });
    }

    private static VaultFile getVaultFile(OldMediaFile mediaFile) {
        VaultFile vaultFile = new VaultFile();

        vaultFile.id = mediaFile.getUid();
        vaultFile.type = VaultFile.Type.FILE;
        vaultFile.name = mediaFile.getFileName();
        vaultFile.created = mediaFile.getCreated();
        vaultFile.duration = mediaFile.getDuration();
        vaultFile.size = mediaFile.getSize();
        vaultFile.anonymous = mediaFile.isAnonymous();
        vaultFile.hash = mediaFile.getHash();
        vaultFile.mimeType = MediaFile.INSTANCE.getMimeTypeForFormatCode(MediaFile.INSTANCE.getFormatCode(mediaFile.getFileName(), FileUtil.getMimeType(mediaFile.getFileName())));
        vaultFile.path = mediaFile.getPath();
        vaultFile.thumb = mediaFile.getThumb();
        vaultFile.metadata = mediaFile.getMetadata();

        return vaultFile;
    }
}
