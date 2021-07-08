package rs.readahead.washington.mobile.media.exo;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.hzontal.tella_vault.VaultFile;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import rs.readahead.washington.mobile.media.MediaFileHandler;


class MediaFileDataSource implements DataSource {
    private final Context context;
    private final VaultFile vaultFile;
    private final @Nullable TransferListener listener;

    private Uri uri;
    private InputStream inputSteam;
    private DataSpec dataSpec = new DataSpec(uri);

    MediaFileDataSource(@NonNull Context context,
                        @NonNull VaultFile vaultFile,
                        @Nullable TransferListener listener) {
        this.context = context;
        this.vaultFile = vaultFile;
        this.listener = listener;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {

    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        uri = dataSpec.uri;

        inputSteam = MediaFileHandler.getStream(vaultFile);

        if (inputSteam == null) {
            close();
            throw new IOException("InputStream not found");
        }

        if (listener != null) {
            listener.onTransferStart(this, dataSpec, false);
        }

        long skipped = inputSteam.skip(dataSpec.position);

        if (skipped != dataSpec.position) {
            throw new IOException("InputStream skip failed");
        }

        long size = MediaFileHandler.getSize(vaultFile);

        if (size - dataSpec.position <= 0) {
            close();
            throw new EOFException();
        }

        return size - dataSpec.position;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        int read = inputSteam.read(buffer, offset, readLength);

        if (read > 0 && listener != null) {
            listener.onBytesTransferred(this, dataSpec, false, read);
        }

        return read;
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close() throws IOException {
        inputSteam.close();
    }
}
