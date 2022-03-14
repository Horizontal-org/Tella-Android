package rs.readahead.washington.mobile.data.entity.mapper;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.MyLocation;
import com.hzontal.tella_vault.VaultFile;

import java.util.ArrayList;
import java.util.Collection;

import rs.readahead.washington.mobile.data.entity.FeedbackEntity;
import rs.readahead.washington.mobile.data.entity.FormMediaFileRegisterEntity;
import rs.readahead.washington.mobile.data.entity.MediaFileEntity;
import rs.readahead.washington.mobile.data.entity.MetadataEntity;
import rs.readahead.washington.mobile.data.entity.uwazi.answer.UwaziLocation;
import rs.readahead.washington.mobile.domain.entity.Feedback;


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

    private MediaFileEntity transform(VaultFile vaultFile) {
        if (vaultFile == null) {
            return null;
        }

        MediaFileEntity mediaFileEntity = new MediaFileEntity();
        mediaFileEntity.id = vaultFile.id;
        mediaFileEntity.path = vaultFile.path;
        mediaFileEntity.uid = vaultFile.id;
        mediaFileEntity.fileName = vaultFile.name;
        mediaFileEntity.metadata = transform(vaultFile.metadata);
        mediaFileEntity.created = vaultFile.created;

        return mediaFileEntity;
    }

    public FormMediaFileRegisterEntity transformMediaFiles(Collection<VaultFile> mediaFiles) {
        FormMediaFileRegisterEntity entity = new FormMediaFileRegisterEntity();

        if (mediaFiles != null) {
            entity.attachments = new ArrayList<>(mediaFiles.size());
            for (VaultFile mediaFile : mediaFiles) {
                entity.attachments.add(transform(mediaFile));
            }
        }

        return entity;
    }

    public UwaziLocation transformMyLocation(MyLocation myLocation) {
        return new UwaziLocation(myLocation.getLatitude(), myLocation.getLongitude(), "");
    }

    public static MyLocation transformUwaziLocation(UwaziLocation uLocation) {
        MyLocation myLocation = new MyLocation();
        myLocation.setLongitude(uLocation.getLon());
        myLocation.setLatitude(uLocation.getLat());
        return  myLocation;
    }

/*
    public List<TrainModule> transform(List<TrainModuleEntity> entities) {
        List<TrainModule> modules = new ArrayList<>(entities.size());

        for (TrainModuleEntity entity: entities) {
            modules.add(transform(entity));
        }

        return modules;
    }

    private TrainModule transform(TrainModuleEntity entity) {
        TrainModule module = new TrainModule();

        module.setId(entity.id);
        module.setName(entity.name);
        module.setUrl(entity.url);
        module.setDownloaded(DownloadState.NOT_DOWNLOADED);
        module.setOrganization(entity.organization);
        module.setType(entity.type);
        module.setSize(entity.size);

        return module;
    }*/
}
