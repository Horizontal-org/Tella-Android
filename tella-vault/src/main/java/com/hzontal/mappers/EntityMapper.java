package com.hzontal.mappers;

import com.hzontal.data.Feedback;
import com.hzontal.data.FeedbackEntity;
import com.hzontal.data.FormVaultFileRegisterEntity;
import com.hzontal.data.MetadataEntity;
import com.hzontal.data.VaultFileEntity;
import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.MyLocation;
import com.hzontal.tella_vault.VaultFile;

import java.util.ArrayList;
import java.util.Collection;


// @Singleton
public class EntityMapper {
    public FeedbackEntity transform(Feedback feedback) {
        FeedbackEntity entity = null;

        if (feedback != null) {
            entity = new FeedbackEntity();
            entity.name = feedback.getName();
            entity.email = feedback.getEmail();
            entity.message = feedback.getMessage();
        }

        return entity;
    }

    public MetadataEntity transform(Metadata metadata) {
        MetadataEntity entity = null;

        if (metadata != null) {
            entity = new MetadataEntity();
            entity.setWifis(metadata.getWifis());
            entity.setCells(metadata.getCells());
            entity.setAmbientTemperature(metadata.getAmbientTemperature());
            entity.setLight(metadata.getLight());
            entity.setTimestamp(metadata.getTimestamp());
            entity.setLocation(transform(metadata.getMyLocation()));
            entity.setInternal(metadata.getInternal());
            //-PM add-
            entity.setIPv6(metadata.getIPv6());
            entity.setIPv4(metadata.getIPv4());
            entity.setLanguage(metadata.getLanguage());
            entity.setNetworkType(metadata.getNetworkType());
            entity.setNetwork(metadata.getNetwork());
            entity.setManufacturer(metadata.getManufacturer());
            entity.setDataType(metadata.getDataType());
            entity.setHardware(metadata.getHardware());
            entity.setScreenSize(metadata.getScreenSize());
            entity.setWifiMac(metadata.getWifiMac());
            entity.setNotes(metadata.getNotes());
            entity.setDeviceID(metadata.getDeviceID());
            entity.setLocale(metadata.getLocale());
            entity.setFileHashSHA256(metadata.getFileHashSHA256());
            entity.setFileName(metadata.getFileName());
        }

        return entity;
    }

    public Metadata transform(MetadataEntity metadataEntity) {
        Metadata metadata = null;

        if (metadataEntity != null) {
            metadata = new Metadata();
            metadata.setWifis(metadataEntity.getWifis());
            metadata.setCells(metadataEntity.getCells());
            metadata.setAmbientTemperature(metadataEntity.getAmbientTemperature());
            metadata.setLight(metadataEntity.getLight());
            metadata.setTimestamp(metadataEntity.getTimestamp());
            metadata.setMyLocation(transform(metadataEntity.getLocation()));
            metadata.setInternal(metadataEntity.getInternal());
            //-PM add-
            metadata.setIPv6(metadataEntity.getIPv6());
            metadata.setIPv4(metadataEntity.getIPv4());
            metadata.setLanguage(metadataEntity.getLanguage());
            metadata.setNetworkType(metadataEntity.getNetworkType());
            metadata.setNetwork(metadataEntity.getNetwork());
            metadata.setManufacturer(metadataEntity.getManufacturer());
            metadata.setDataType(metadataEntity.getDataType());
            metadata.setHardware(metadataEntity.getHardware());
            metadata.setScreenSize(metadataEntity.getScreenSize());
            metadata.setWifiMac(metadataEntity.getWifiMac());
            metadata.setNotes(metadataEntity.getNotes());
            metadata.setDeviceID(metadataEntity.getDeviceID());
            metadata.setLocale(metadataEntity.getLocale());
            metadata.setFileHashSHA256(metadataEntity.getFileHashSHA256());
            metadata.setFileName(metadataEntity.getFileName());
        }

        return metadata;
    }

    private MetadataEntity.LocationEntity transform(MyLocation location) {
        MetadataEntity.LocationEntity entity = null;

        if (location != null) {
            entity = new MetadataEntity.LocationEntity();
            entity.setLatitude(location.getLatitude());
            entity.setLongitude(location.getLongitude());
            entity.setAccuracy(location.getAccuracy());
            entity.setAltitude(location.getAltitude());
            entity.setTimestamp(location.getTimestamp());
            entity.setProvider(location.getProvider());
            entity.setSpeed(location.getSpeed());
        }

        return entity;
    }

    private MyLocation transform(MetadataEntity.LocationEntity locationEntity) {
        MyLocation location = null;

        if (locationEntity != null) {
            location = new MyLocation();
            location.setLatitude(locationEntity.getLatitude());
            location.setLongitude(locationEntity.getLongitude());
            location.setAccuracy(locationEntity.getAccuracy());
            location.setAltitude(locationEntity.getAltitude());
            location.setTimestamp(locationEntity.getTimestamp());
            location.setProvider(locationEntity.getProvider());
            location.setSpeed(locationEntity.getSpeed());
        }

        return location;
    }

    private VaultFileEntity transform(VaultFile vaultFile) {
        if (vaultFile == null) {
            return null;
        }

        VaultFileEntity mediaFileEntity = new VaultFileEntity();
        mediaFileEntity.id = vaultFile.id;
        mediaFileEntity.path = vaultFile.path;
        mediaFileEntity.uid = vaultFile.id;
        mediaFileEntity.fileName = vaultFile.name;
        mediaFileEntity.metadata = transform(vaultFile.metadata);
        mediaFileEntity.created = vaultFile.created;

        return mediaFileEntity;
    }

    public FormVaultFileRegisterEntity transformMediaFiles(Collection<VaultFile> mediaFiles) {
        FormVaultFileRegisterEntity entity = new FormVaultFileRegisterEntity();

        if (mediaFiles != null) {
            entity.attachments = new ArrayList<>(mediaFiles.size());
            for (VaultFile mediaFile: mediaFiles) {
                entity.attachments.add(transform(mediaFile));
            }
        }

        return entity;
    }
}
