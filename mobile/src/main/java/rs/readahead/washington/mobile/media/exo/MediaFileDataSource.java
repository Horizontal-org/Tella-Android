package rs.readahead.washington.mobile.media.exo;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import rs.readahead.washington.mobile.domain.entity.MediaFile;
import rs.readahead.washington.mobile.media.MediaFileHandler;


class MediaFileDataSource implements DataSource {
    private final Context context;
    private final MediaFile mediaFile;
    private final @Nullable TransferListener<? super DataSource> listener;

    private Uri uri;
    private InputStream inputSteam;


    MediaFileDataSource(@NonNull Context context,
                        @NonNull MediaFile mediaFile,
                        @Nullable TransferListener<? super DataSource> listener) {
        this.context = context;
        this.mediaFile = mediaFile;
        this.listener = listener;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        uri = dataSpec.uri;

        inputSteam = MediaFileHandler.getStream(context, mediaFile);

        if (inputSteam == null) {
            close();
            throw new IOException("InputStream not found");
        }

        if (listener != null) {
            listener.onTransferStart(this, dataSpec);
        }

        long skipped = inputSteam.skip(dataSpec.position);

        if (skipped != dataSpec.position) {
            throw new IOException("InputStream skip failed");
        }

        long size = MediaFileHandler.getSize(context, mediaFile);

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
            listener.onBytesTransferred(this, read);
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
