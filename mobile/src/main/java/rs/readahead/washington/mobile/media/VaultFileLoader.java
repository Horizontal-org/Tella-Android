package rs.readahead.washington.mobile.media;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.UriLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;

public class VaultFileLoader<Data> {
    private final MediaFileHandler mediaFileHandler;
   /* private VaultFile vaultFile;
    private VaultFileLoaderModel.LoadType loadType;*/

    private final VaultFileLoader.VaultFetcherFactory<Data> factory;


    public interface VaultFetcherFactory<Data> {
        DataFetcher<Data> build(VaultFileLoaderModel model);
    }

    public VaultFileLoader(MediaFileHandler mediaFileHandler, VaultFetcherFactory<Data> factory) {
        this.mediaFileHandler = mediaFileHandler;
        this.factory = factory;
    }

   public DataFetcher<InputStream> getResourceFetcher(VaultFileLoaderModel model, int width, int height) {
        return new VaultFileDataFetcher(mediaFileHandler, model);
    }

    public DataFetcher<InputStream> build(VaultFileLoaderModel model) {
        return new VaultFileDataFetcher(mediaFileHandler, model);
    }
}
