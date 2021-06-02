package rs.readahead.washington.mobile.presentation.entity.mapper;

import androidx.annotation.NonNull;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.MyLocation;
import com.hzontal.tella_vault.VaultFile;

import java.util.LinkedHashMap;

import rs.readahead.washington.mobile.presentation.entity.PublicMetadata;
import rs.readahead.washington.mobile.util.StringUtils;


public class PublicMetadataMapper {
    private static PublicMetadata transform(@NonNull VaultFile vaultFile) {
        PublicMetadata metadata = new PublicMetadata();

        metadata.fileHash = vaultFile.hash;
        metadata.filePath = vaultFile.path;
        metadata.fileName = vaultFile.name;

        Metadata mfmd = vaultFile.metadata;

        if (mfmd == null) {
            return metadata;
        }

        metadata.cells = mfmd.getCells();
        metadata.wifis = mfmd.getWifis();
        metadata.timestamp = mfmd.getTimestamp();
        metadata.ambientTemperature = mfmd.getAmbientTemperature();
        metadata.light = mfmd.getLight();

        if (mfmd.getMyLocation() != null) {
            metadata.location = transform(mfmd.getMyLocation());
        }

        metadata.locale = mfmd.getLocale();
        metadata.IPv6 = mfmd.getIPv6();
        metadata.IPv4 = mfmd.getIPv4();
        metadata.language = mfmd.getLanguage();
        metadata.networkType = mfmd.getNetworkType();
        metadata.network = mfmd.getNetwork();
        metadata.manufacturer = mfmd.getManufacturer();
        metadata.dataType = mfmd.getDataType();
        metadata.hardware = mfmd.getHardware();
        metadata.screenSize = mfmd.getScreenSize();
        metadata.wifiMac = mfmd.getWifiMac();
        metadata.notes = mfmd.getNotes();
        metadata.deviceID = mfmd.getDeviceID();

        return metadata;
    }

    public static LinkedHashMap<String, String> transformToMap(@NonNull VaultFile vaultFile) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        PublicMetadata publicMetadata = transform(vaultFile);

        metadata.put("File hash", rc(publicMetadata.fileHash));
        metadata.put("File path", rc(publicMetadata.filePath));
        metadata.put("File name", rc(publicMetadata.fileName));

        metadata.put("Cells", rc(StringUtils.join(" ", publicMetadata.cells)));
        metadata.put("WiFis", rc(StringUtils.join(" ", publicMetadata.wifis)));
        metadata.put("Timestamp", rc(publicMetadata.timestamp));
        metadata.put("Ambient temperature", rc(publicMetadata.ambientTemperature));
        metadata.put("Light", rc(publicMetadata.light));

        if (publicMetadata.location != null) {
            metadata.put("Location accuracy", rc(publicMetadata.location.accuracy));
            metadata.put("Location altitude", rc(publicMetadata.location.altitude));
            metadata.put("Location latitude", rc(publicMetadata.location.latitude));
            metadata.put("Location longitude", rc(publicMetadata.location.longitude));
            metadata.put("Location timestamp", rc(publicMetadata.location.timestamp));
            metadata.put("Location provider", rc(publicMetadata.location.provider));
            metadata.put("Location speed", rc(publicMetadata.location.speed));
        }

        metadata.put("Locale", rc(publicMetadata.locale));
        metadata.put("IPv6", rc(publicMetadata.IPv6));
        metadata.put("IPv4", rc(publicMetadata.IPv4));
        metadata.put("Language", rc(publicMetadata.language));
        metadata.put("Network type", rc(publicMetadata.networkType));
        metadata.put("Network", rc(publicMetadata.network));
        metadata.put("Manufacturer", rc(publicMetadata.manufacturer));
        metadata.put("Data type", rc(publicMetadata.dataType));
        metadata.put("Hardware", rc(publicMetadata.hardware));
        metadata.put("Screen size", rc(publicMetadata.screenSize));
        metadata.put("WiFi MAC", rc(publicMetadata.wifiMac));
        metadata.put("Device ID", rc(publicMetadata.deviceID));

        return metadata;
    }

    private static PublicMetadata.PublicLocation transform(@NonNull MyLocation myLocation) {
        PublicMetadata.PublicLocation location = new PublicMetadata.PublicLocation();

        location.accuracy = myLocation.getAccuracy();
        location.altitude = myLocation.getAltitude();
        location.latitude = myLocation.getLatitude();
        location.longitude = myLocation.getLongitude();
        location.timestamp = myLocation.getTimestamp();
        location.provider = myLocation.getProvider();
        location.speed = myLocation.getSpeed();

        return location;
    }

    private static String rc(String str) {
        if (str == null) {
            return "";
        }

        return str.replace(",", " ");
    }

    private static String rc(Double d) {
        if (d == null) {
            return "";
        }

        return d.toString().replace(",", ".");
    }

    private static String rc(Float f) {
        if (f == null) {
            return "";
        }

        return f.toString().replace(",", ".");
    }

    private static String rc(long l) {
        return "" + l;
    }
}
