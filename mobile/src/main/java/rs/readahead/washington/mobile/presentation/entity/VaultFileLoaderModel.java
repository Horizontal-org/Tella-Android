package rs.readahead.washington.mobile.presentation.entity;

import com.hzontal.tella_vault.VaultFile;

public class VaultFileLoaderModel {

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

    public enum LoadType {
        ORIGINAL,
        THUMBNAIL
    }
}
