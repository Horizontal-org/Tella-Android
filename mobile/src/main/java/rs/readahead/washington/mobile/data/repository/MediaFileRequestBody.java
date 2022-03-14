package rs.readahead.washington.mobile.data.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hzontal.tella_vault.VaultFile;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import rs.readahead.washington.mobile.domain.entity.IProgressListener;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.util.FileUtil;
import timber.log.Timber;


public class MediaFileRequestBody extends RequestBody {
    protected final VaultFile mediaFile;
    private final MediaType contentType;
    private final IProgressListener listener;

    public  MediaFileRequestBody(VaultFile mediaFile) {
        this(mediaFile, null);
    }

   public MediaFileRequestBody(VaultFile mediaFile, @Nullable IProgressListener progressListener) {
        String mime = mediaFile.mimeType;

        if (TextUtils.isEmpty(mime)) {
            mime = "application/octet-stream";
        }
        this.contentType = MediaType.parse(mime);
        this.mediaFile = mediaFile;
        this.listener = progressListener;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() {
        return mediaFile.size;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        Source source = null;
        InputStream is = null;

        try {
            is = getInputStream();

            if (is == null) {
                Timber.d("MediaFileHandler.getStream(%s) returned null", mediaFile.id);
                return;
            }

            source = Okio.source(is);

            // writeAll method from RealBufferedSink
            long totalBytesRead = 0;
            for (long readCount; (readCount = source.read(sink.buffer(), 8192)) != -1; ) {
                totalBytesRead += readCount;
                sink.emitCompleteSegments();

                if (listener != null) {
                    listener.onProgressUpdate(totalBytesRead, contentLength());
                }
            }
        } catch (Exception e) {
            Timber.d(e);
            throw e;
        } finally {
            Util.closeQuietly(source);
            Util.closeQuietly(is);
        }
    }

    protected InputStream getInputStream() throws IOException {
        return MediaFileHandler.getStream(mediaFile);
    }

    /* final class CountingSink extends ForwardingSink {
        private long bytesWritten = 0;

        CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;

            if (listener != null) {
                listener.onProgressUpdate(bytesWritten, contentLength());
            }
        }
    } */
}
