package rs.readahead.washington.mobile.data.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class MetadataEntity {
    @SerializedName("cells")
    private List<String> cells;

    @SerializedName("wifis")
    private List<String> wifis;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("ambientTemperature")
    private Float ambientTemperature;

    @SerializedName("light")
    private Float light;

    @SerializedName("location")
    private LocationEntity location;

    @SerializedName("internal")
    private boolean internal;

    @SerializedName("locale")
    private String locale;

    @SerializedName("IPv6")
    private String IPv6;

    @SerializedName("IPv4")
    private String IPv4;

    @SerializedName("language")
    private String language;

    @SerializedName("networkType")
    private String networkType;

    @SerializedName("network")
    private String network;

    @SerializedName("manufacturer")
    private String manufacturer;

    @SerializedName("dataType")
    private String dataType;

    @SerializedName("hardware")
    private String hardware;

    @SerializedName("screenSize")
    private String screenSize;

    @SerializedName("wifiMac")
    private String wifiMac;

    @SerializedName("notes")
    private String notes;

    @SerializedName("deviceID")
    private String deviceID;

    @SerializedName("locationTime")
    private String locationTime;

    @SerializedName("fileModified")
    private String fileModified;

    @SerializedName("proofGenerated")
    private String proofGenerated;

    @SerializedName("filePath")
    private String filePath;

    @SerializedName("fileHashSHA256")
    private String fileHashSHA256;

    @SerializedName("cellInfo")
    private String cellInfo;

    public List<String> getCells() {
        return cells;
    }

    public void setCells(List<String> cells) {
        this.cells = cells;
    }

    public List<String> getWifis() {
        return wifis;
    }

    public void setWifis(List<String> wifis) {
        this.wifis = wifis;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getWifiMac() {
        return wifiMac;
    }

    public void setWifiMac(String wifiMac) {
        this.wifiMac = wifiMac;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getProofGenerated() {
        return proofGenerated;
    }

    public void setProofGenerated(String proofGenerated) {
        this.proofGenerated = proofGenerated;
    }

    public String getFileModified() {
        return fileModified;
    }

    public void setFileModified(String fileModified) {
        this.fileModified = fileModified;
    }

    public String getLocationTime() {
        return locationTime;
    }

    public void setLocationTime(String locationTime) {
        this.locationTime = locationTime;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(String screenSize) {
        this.screenSize = screenSize;
    }

    public String getHardware() {
        return hardware;
    }

    public void setHardware(String hardware) {
        this.hardware = hardware;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getIPv4() {
        return IPv4;
    }

    public void setIPv4(String IPv4) {
        this.IPv4 = IPv4;
    }

    public String getIPv6() {
        return IPv6;
    }

    public void setIPv6(String IPv6) {
        this.IPv6 = IPv6;
    }

    public String getFileHashSHA256() {
        return fileHashSHA256;
    }

    public void setFileHashSHA256(String fileHashSHA256) {
        this.fileHashSHA256 = fileHashSHA256;
    }

    public String getCellInfo() {
        return cellInfo;
    }

    public void setCellInfo(String cellInfo) {
        this.cellInfo = cellInfo;
    }

    public Float getAmbientTemperature() {
        return ambientTemperature;
    }

    public void setAmbientTemperature(Float ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }

    public Float getLight() {
        return light;
    }

    public void setLight(Float light) {
        this.light = light;
    }

    public LocationEntity getLocation() {
        return location;
    }

    public void setLocation(LocationEntity location) {
        this.location = location;
    }


    public static class LocationEntity {
        @SerializedName("latitude")
        private double latitude;

        @SerializedName("longitude")
        private double longitude;

        @SerializedName("altitude")
        private Double altitude;

        @SerializedName("accuracy")
        private Float accuracy;

        @SerializedName("timestamp")
        private long timestamp;

        @SerializedName("speed")
        private Float speed;

        @SerializedName("provider")
        private String provider;


        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public Double getAltitude() {
            return altitude;
        }

        public void setAltitude(Double altitude) {
            this.altitude = altitude;
        }

        public Float getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(Float accuracy) {
            this.accuracy = accuracy;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public Float getSpeed() {
            return speed;
        }

        public void setSpeed(Float speed) {
            this.speed = speed;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }
}
