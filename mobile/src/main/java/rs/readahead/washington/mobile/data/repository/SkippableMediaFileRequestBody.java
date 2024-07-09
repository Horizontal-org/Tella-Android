package rs.readahead.washington.mobile.data.repository;

import androidx.annotation.Nullable;

import com.hzontal.tella_vault.VaultFile;

import java.io.IOException;
import java.io.InputStream;

import rs.readahead.washington.mobile.domain.entity.IProgressListener;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import timber.log.Timber;


public class SkippableMediaFileRequestBody extends MediaFileRequestBody {
    private static final int CHUNK_SIZE = 128 * 1024;
    private byte[] buffer = new byte[CHUNK_SIZE];

    private long skip;


    public SkippableMediaFileRequestBody(VaultFile mediaFile, long skip, @Nullable IProgressListener progressListener) {
        super(mediaFile, progressListener);

        this.skip = skip;
    }

    @Override
    public long contentLength() {
        return mediaFile.size - skip;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        InputStream is = MediaFileHandler.getStream(mediaFile);

        if (is != null) {
            long skipped = skipBytes(is, skip);

            if (skipped != skip) {
                Timber.d("Unable to skip required bytes %d, skipped %s", skip, skipped);
                throw new IOException("Unable to skip required bytes");
            }
        }

        return is;
    }

    private long skipBytes(InputStream inputStream, long numBytes) throws IOException {
        if (numBytes <= 0) {
            return 0;
        }

        long n = numBytes;
        int nr;

        while (n > 0) {
            // todo: implement AES RandomAccessFile - it is possible with CTR/NoPadding
            nr = inputStream.read(buffer, 0, (int) Math.min(CHUNK_SIZE, n));
            if (nr < 0) {
                break;
            }
            n -= nr;
        }

        return numBytes - n;
    }
}
