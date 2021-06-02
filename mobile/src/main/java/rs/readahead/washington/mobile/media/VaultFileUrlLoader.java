package rs.readahead.washington.mobile.media;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.stream.StreamModelLoader;

import java.io.InputStream;

import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;

public class VaultFileUrlLoader implements StreamModelLoader<VaultFileLoaderModel> {
    private Context context;
    private MediaFileHandler mediaFileHandler;


    public VaultFileUrlLoader(Context context, MediaFileHandler mediaFileHandler) {
        this.context = context.getApplicationContext();
        this.mediaFileHandler = mediaFileHandler;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(VaultFileLoaderModel model, int width, int height) {
        return new VaultFileDataFetcher(context, mediaFileHandler, model);
    }
}
