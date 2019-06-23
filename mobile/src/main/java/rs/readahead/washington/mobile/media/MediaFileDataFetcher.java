package rs.readahead.washington.mobile.media;

import android.content.Context;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

import rs.readahead.washington.mobile.presentation.entity.MediaFileLoaderModel;


class MediaFileDataFetcher implements DataFetcher<InputStream> {
    private MediaFileHandler mediaFileHandler;
    private MediaFileLoaderModel model;
    private Context context;
    private boolean cancelled = false;
    private InputStream inputStream;


    MediaFileDataFetcher(Context context, MediaFileHandler mediaFileHandler, MediaFileLoaderModel mediaFileLoaderModel) {
        this.context = context;
        this.mediaFileHandler = mediaFileHandler;
        this.model = mediaFileLoaderModel;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        if (model == null) {
            return null;
        }

        if (cancelled) {
            return null;
        }

        if (model.getLoadType() == MediaFileLoaderModel.LoadType.THUMBNAIL) {
            return inputStream = mediaFileHandler.getThumbnailStream(context, model.getMediaFile());
        }

        if (model.getLoadType() == MediaFileLoaderModel.LoadType.ORIGINAL) {
            return inputStream = MediaFileHandler.getStream(context, model.getMediaFile());
        }

        return null;
    }

    @Override
    public void cleanup() {
        cancel();
    }

    @Override
    public String getId() {
        return model.getMediaFile().getFileName();
    }

    @Override
    public void cancel() {
        cancelled = true;

        if (inputStream != null) {
            try {
                inputStream.close(); // interrupts decode if any
                inputStream = null;
            } catch (IOException ignore) {
            }
        }
    }
}
