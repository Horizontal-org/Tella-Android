package rs.readahead.washington.mobile.util;

import android.content.Context;
import android.os.AsyncTask;

import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.database.VaultDataSource;

import java.util.List;

import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.OldMediaFile;
import timber.log.Timber;

public class TellaUpdater {

    public static void updateV2(Context context, byte[] key){
        DataSource dataSource = DataSource.getInstance(context,key);
        VaultDataSource vaultDataSource = VaultDataSource.getInstance(context,key);

        AsyncTask.execute(() -> {
            List<OldMediaFile> allMediaFiles = dataSource.listOldMediaFiles().blockingGet();

            for (OldMediaFile mediaFile: allMediaFiles) {
                vaultDataSource.create(null,getVaultFile(mediaFile) );
            }

            Preferences.setUpdateTella2(false);
        });
    }

    private static VaultFile getVaultFile(OldMediaFile mediaFile){
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
        mediaFile.setMetadata(mediaFile.getMetadata());

        return vaultFile;
    }

}
