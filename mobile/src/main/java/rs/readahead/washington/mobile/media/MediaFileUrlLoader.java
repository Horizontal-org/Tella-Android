package rs.readahead.washington.mobile.media;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.stream.StreamModelLoader;

import java.io.InputStream;

import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;


public class MediaFileUrlLoader implements StreamModelLoader<MediaFileLoaderModel> {
    private Context context;
    private MediaFileHandler mediaFileHandler;


    public MediaFileUrlLoader(Context context, MediaFileHandler mediaFileHandler) {
        this.context = context.getApplicationContext();
        this.mediaFileHandler = mediaFileHandler;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(MediaFileLoaderModel model, int width, int height) {
        return new MediaFileDataFetcher(context, mediaFileHandler, model);
    }
}
