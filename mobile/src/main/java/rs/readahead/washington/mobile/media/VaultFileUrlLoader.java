package rs.readahead.washington.mobile.media;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.StreamLocalUriFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.UriLoader;
import com.bumptech.glide.load.model.UrlUriLoader;
//import com.bumptech.glide.load.model.stream.StreamModelLoader;

import java.io.InputStream;

import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;

public class VaultFileUrlLoader<Data> implements ModelLoader<Uri, Data> {
    private Context context;
    private MediaFileHandler mediaFileHandler;

    private VaultFileLoaderModel model;
   // private final VaultFileUrlLoader.LocalUriFetcherFactory<Data> factory;
    @Nullable
    @Override
    public LoadData<Data> buildLoadData(@NonNull Uri uri, int width, int height, @NonNull Options options) {
        return null;
    }
   /* public VaultFileUrlLoader(VaultFileUrlLoader.LocalUriFetcherFactory<Data> factory) {
        this.factory = factory;
    }*/
    @Override
    public boolean handles(@NonNull Uri uri) {
        return false;
    }

    public VaultFileUrlLoader(Context context, MediaFileHandler mediaFileHandler, VaultFileLoaderModel model) {
        this.context = context.getApplicationContext();
        this.mediaFileHandler = mediaFileHandler;
        this.model = model;
    }

    public VaultFileUrlLoader(Context context, MediaFileHandler mediaFileHandler) {
        this.context = context.getApplicationContext();
        this.mediaFileHandler = mediaFileHandler;
    }


   public DataFetcher<InputStream> getResourceFetcher(VaultFileLoaderModel model, int width, int height) {
        return new VaultFileDataFetcher(mediaFileHandler, model);
    }

    public DataFetcher<InputStream> build(VaultFileLoaderModel model) {
        return new VaultFileDataFetcher(mediaFileHandler, model);
    }
}
