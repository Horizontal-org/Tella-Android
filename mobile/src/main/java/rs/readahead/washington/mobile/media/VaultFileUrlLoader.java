package rs.readahead.washington.mobile.media;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.stream.StreamModelLoader;

import java.io.InputStream;

public class VaultFileUrlLoader implements StreamModelLoader<VaultFileUrlLoader> {
    private Context context;
    private MediaFileHandler mediaFileHandler;


    public VaultFileUrlLoader(Context context, MediaFileHandler mediaFileHandler) {
        this.context = context.getApplicationContext();
        this.mediaFileHandler = mediaFileHandler;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(VaultFileUrlLoader model, int width, int height) {
        return new MediaFileDataFetcher(context, mediaFileHandler, model);
    }
}
