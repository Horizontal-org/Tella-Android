package rs.readahead.washington.mobile.domain.entity;

import java.io.Serializable;
import java.util.List;


public final class Metadata implements Serializable {
    private List<String> cells; // sync
    private List<String> wifis; // async
    private long timestamp; // sync, UTC millisecond timestamp
    private Float ambientTemperature; // semi-sync, nullable
    private Float light; // semi-sync, nullable
    private MyLocation myLocation; // async, nullable
    private boolean internal;
    // -PM add-
    private String fileName;
    private String locale;
    private String IPv6;
    private String IPv4;
    private String language;
    private String networkType;
    private String network;
    private String manufacturer;
    private String dataType;
    private String hardware;
    private String screenSize;
    private String wifiMac;
    private String notes;
    private String deviceID;
    private String fileHashSHA256; // todo: remove this one day

    public Metadata() {
    }

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

    public MyLocation getMyLocation() {
        return myLocation;
    }

    public void setMyLocation(MyLocation location) {
        this.myLocation = location;
    }

    public boolean getInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    //-PM add-
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
}
