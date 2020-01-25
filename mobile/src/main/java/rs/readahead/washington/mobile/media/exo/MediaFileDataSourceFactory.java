package rs.readahead.washington.mobile.media.exo;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import rs.readahead.washington.mobile.domain.entity.MediaFile;


public class MediaFileDataSourceFactory implements DataSource.Factory {
    private final Context context;
    private final MediaFile mediaFile;
    private final TransferListener listener;


    public MediaFileDataSourceFactory(
            @NonNull Context context,
            @NonNull MediaFile mediaFile,
            @Nullable TransferListener listener) {
        this.mediaFile = mediaFile;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public DataSource createDataSource() {
        return new MediaFileDataSource(context, mediaFile, listener);
    }
}
