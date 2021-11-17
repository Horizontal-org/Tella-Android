package rs.readahead.washington.mobile.util;

import android.content.Context;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.tella_vault.database.VaultDataSource;

import java.util.List;

import rs.readahead.washington.mobile.data.database.DataSource;
import rs.readahead.washington.mobile.domain.entity.OldMediaFile;
import timber.log.Timber;

public class TellaUpdater {

    public static boolean updateV2(Context context, byte[] key){
        Timber.d("++++ updateV2");
        DataSource dataSource = DataSource.getInstance(context,key);
        VaultDataSource vaultDataSource = VaultDataSource.getInstance(context,key);

        List<OldMediaFile> allMediaFiles = dataSource.listOldMediaFiles().blockingGet();

        for (OldMediaFile mediaFile: allMediaFiles) {
            vaultDataSource.create(null,getVaultFile(mediaFile) );
        }
        return true;
    }

    private static VaultFile getVaultFile(OldMediaFile mediaFile){
        Timber.d("++++ getVaultFile name %s, created %d, size %d path %s",mediaFile.getFileName(), mediaFile.getCreated(), mediaFile.getSize(), mediaFile.getPath() );
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
