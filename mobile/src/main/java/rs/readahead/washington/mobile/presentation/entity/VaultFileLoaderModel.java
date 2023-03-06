package rs.readahead.washington.mobile.presentation.entity;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.hzontal.tella_vault.VaultFile;

public class VaultFileLoaderModel implements ModelLoader{

    private VaultFile vaultFile;
    private VaultFileLoaderModel.LoadType loadType;


    public VaultFileLoaderModel(VaultFile vaultFile, VaultFileLoaderModel.LoadType loadType) {
        this.vaultFile = vaultFile;
        this.loadType = loadType;
    }

    public VaultFile getMediaFile() {
        return vaultFile;
    }

    public VaultFileLoaderModel.LoadType getLoadType() {
        return loadType;
    }

    @Nullable
    @Override
    public LoadData buildLoadData(@NonNull Object o, int width, int height, @NonNull Options options) {
        return null;
    }

    @Override
    public boolean handles(@NonNull Object o) {
        return false;
    }

    public enum LoadType {
        ORIGINAL,
        THUMBNAIL
    }
}
