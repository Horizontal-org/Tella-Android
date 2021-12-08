package rs.readahead.washington.mobile.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.database.VaultDataSource;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.OldMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.CollectInstanceVaultFile;

public class TellaUpgrader {

    public static void upgradeV2(Context context, byte[] key) {
        DataSource dataSource = DataSource.getInstance(context, key);
        VaultDataSource vaultDataSource = VaultDataSource.getInstance(context, key);
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            {
                List<OldMediaFile> allMediaFiles = dataSource.listOldMediaFiles().blockingGet();
                for (OldMediaFile mediaFile : allMediaFiles) {
                    vaultDataSource.create(null, getVaultFile(mediaFile));
                }
                List<CollectInstanceVaultFile> collectInstanceVaultFiles = dataSource.getCollectInstanceVaultFilesDB();
                for (CollectInstanceVaultFile instanceVaultFile : collectInstanceVaultFiles) {
                    dataSource.insertCollectInstanceVaultFile(instanceVaultFile);
                }
            }
            new Handler(Looper.getMainLooper()).post(() -> Preferences.setUpgradeTella2(false));
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
        vaultFile.mimeType = FileUtil.getMimeType(mediaFile.getFileName());
        vaultFile.path = mediaFile.getPath();
        vaultFile.thumb = mediaFile.getThumb();
        mediaFile.setMetadata(mediaFile.getMetadata());

        return vaultFile;
    }

}
