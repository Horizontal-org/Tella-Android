package org.horizontal.tella.mobile.media.exo;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.hzontal.tella_vault.VaultFile;


public class MediaFileDataSourceFactory implements DataSource.Factory {
    private final Context context;
    private final VaultFile vaultFile;
    private final TransferListener listener;


    public MediaFileDataSourceFactory(
            @NonNull Context context,
            @NonNull VaultFile vaultFile,
            @Nullable TransferListener listener) {
        this.vaultFile = vaultFile;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public DataSource createDataSource() {
        return new MediaFileDataSource(context, vaultFile, listener);
    }
}
