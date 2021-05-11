package rs.readahead.washington.mobile.media;

import android.content.Context;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.IOException;
import java.io.InputStream;

import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;

public class VaultFileDataFetcher implements DataFetcher<InputStream> {
    private MediaFileHandler mediaFileHandler;
    private VaultFileLoaderModel model;
    private Context context;
    private boolean cancelled = false;
    private InputStream inputStream;


    VaultFileDataFetcher(Context context, MediaFileHandler mediaFileHandler, VaultFileLoaderModel vaultFileLoaderModel) {
        this.context = context;
        this.mediaFileHandler = mediaFileHandler;
        this.model = vaultFileLoaderModel;
    }

    @Override
    public InputStream loadData(Priority priority) {
        if (model == null) {
            return null;
        }

        if (cancelled) {
            return null;
        }

        if (model.getLoadType() == VaultFileLoaderModel.LoadType.THUMBNAIL) {
            return inputStream = mediaFileHandler.getThumbnailStream(context, model.getMediaFile());
        }

        if (model.getLoadType() == VaultFileLoaderModel.LoadType.ORIGINAL) {
            return inputStream = MediaFileHandler.getStream(model.getMediaFile());
        }

        return null;
    }

    @Override
    public void cleanup() {
        cancel();
    }

    @Override
    public String getId() {
        return model.getMediaFile().id;
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
